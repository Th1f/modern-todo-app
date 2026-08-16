package com.todoapp.backend.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;
import com.todoapp.backend.error.ApiExceptionHandler;
import com.todoapp.backend.error.ConflictException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

@WebMvcTest(CategoryController.class)
@Import({ SecurityConfig.class, SecurityBeans.class, ApiExceptionHandler.class })
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserRepository userRepository;

    private Category category(long id, String name, String color) {
        User owner = new User();
        owner.setUsername("alice");

        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setColor(color);
        category.setOwner(owner);
        return category;
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());

        verify(categoryService, never()).listFor(any());
    }

    @Test
    @WithMockUser("alice")
    void listPassesTheSessionUsername() throws Exception {
        when(categoryService.listFor("alice"))
                .thenReturn(List.of(
                        category(1L, "Errands", "#F59E0B"),
                        category(2L, "Work", "#3B82F6")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Errands"))
                .andExpect(jsonPath("$[0].owner").doesNotExist());
    }

    @Test
    @WithMockUser("alice")
    void createReturns201AndForwardsOnlyNameAndColour() throws Exception {
        when(categoryService.create("alice", "Fitness", "#EF4444"))
                .thenReturn(category(4L, "Fitness", "#EF4444"));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":99,\"name\":\"Fitness\",\"color\":\"#EF4444\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fitness"));

        // The id in the body is structurally unable to reach the service.
        verify(categoryService).create("alice", "Fitness", "#EF4444");
    }

    @Test
    @WithMockUser("alice")
    void createRejectsBlankNameBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"color\":\"#3B82F6\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser("alice")
    void createRejectsNonHexColourBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Fitness\",\"color\":\"red\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser("alice")
    void conflictBecomes409() throws Exception {
        when(categoryService.create("alice", "Work", "#3B82F6"))
                .thenThrow(new ConflictException("\"Work\" already exists"));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Work\",\"color\":\"#3B82F6\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser("alice")
    void updateReturnsTheUpdatedCategory() throws Exception {
        when(categoryService.update(eq("alice"), eq(1L), any(CategoryUpdate.class)))
                .thenReturn(category(1L, "Job", "#3B82F6"));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Job\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Job"));
    }

    @Test
    @WithMockUser("alice")
    void updateRejectsNonHexColourBeforeReachingTheService() throws Exception {
        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"not-a-colour\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).update(any(), any(), any());
    }

    @Test
    @WithMockUser("john")
    void notFoundBecomes404() throws Exception {
        when(categoryService.update(eq("john"), eq(1L), any(CategoryUpdate.class)))
                .thenThrow(new NotFoundException("Category 1"));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("alice")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 1L)).andExpect(status().isNoContent());

        verify(categoryService).delete("alice", 1L);
    }

    @Test
    @WithMockUser("alice")
    void deleteConflictBecomes409() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("still in use"))
                .when(categoryService).delete("alice", 1L);

        mockMvc.perform(delete("/api/categories/{id}", 1L)).andExpect(status().isConflict());
    }
}
