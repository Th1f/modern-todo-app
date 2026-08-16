package com.todoapp.backend.task;

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

import com.todoapp.backend.category.Category;
import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;
import com.todoapp.backend.error.ApiExceptionHandler;
import com.todoapp.backend.error.ConflictException;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.error.NotAuthenticatedException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

/**
 * The HTTP contract only: routing, status codes, and serialisation. Business
 * rules live in TaskServiceTest.
 */
@WebMvcTest(TaskController.class)
@Import({ SecurityConfig.class, SecurityBeans.class, ApiExceptionHandler.class })
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    // Required by SecurityBeans' UserDetailsService, not by the controller.
    @MockitoBean
    private UserRepository userRepository;

    private Task task(long id, String name) {
        User owner = new User();
        owner.setUsername("alice");

        Category category = new Category();
        category.setId(1L);
        category.setName("Work");
        category.setColor("#3B82F6");
        category.setOwner(owner);

        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setCategory(category);
        task.setOwner(owner);
        return task;
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());

        verify(taskService, never()).listFor(any(), any());
    }

    @Test
    @WithMockUser("alice")
    void listPassesTheSessionUsernameAndNoFilter() throws Exception {
        when(taskService.listFor("alice", null)).thenReturn(List.of(task(1L, "Buy milk")));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Buy milk"))
                .andExpect(jsonPath("$[0].isDone").value(false))
                .andExpect(jsonPath("$[0].owner").doesNotExist());
    }

    @Test
    @WithMockUser("alice")
    void listForwardsTheCategoryParam() throws Exception {
        when(taskService.listFor("alice", "Work")).thenReturn(List.of(task(1L, "Buy milk")));

        mockMvc.perform(get("/api/tasks").param("category", "Work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(taskService).listFor("alice", "Work");
    }

    @Test
    @WithMockUser("alice")
    void createReturns201() throws Exception {
        when(taskService.create(eq("alice"), any(TaskRequest.class)))
                .thenReturn(task(1L, "Buy milk"));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Buy milk\",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Buy milk"));
    }

    @Test
    @WithMockUser("alice")
    void createRejectsBlankNameBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).create(any(), any());
    }

    @Test
    @WithMockUser("ghost")
    void notAuthenticatedBecomes401() throws Exception {
        when(taskService.create(eq("ghost"), any(TaskRequest.class)))
                .thenThrow(new NotAuthenticatedException("ghost"));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Buy milk\",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser("alice")
    void invalidRequestBecomes400() throws Exception {
        when(taskService.create(eq("alice"), any(TaskRequest.class)))
                .thenThrow(new InvalidRequestException("Category 99 is not yours"));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sneaky\",\"categoryId\":99,\"isDone\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("alice")
    void conflictBecomes409() throws Exception {
        when(taskService.update(eq("alice"), eq(1L), any(TaskUpdate.class)))
                .thenThrow(new ConflictException("clash"));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser("alice")
    void updateReturnsTheUpdatedTask() throws Exception {
        when(taskService.update(eq("alice"), eq(1L), any(TaskUpdate.class)))
                .thenReturn(task(1L, "Renamed"));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    @WithMockUser("john")
    void notFoundBecomes404() throws Exception {
        when(taskService.update(eq("john"), eq(1L), any(TaskUpdate.class)))
                .thenThrow(new NotFoundException("Task 1"));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("alice")
    void toggleReturnsTheTask() throws Exception {
        Task toggled = task(1L, "Buy milk");
        toggled.setDone(true);
        when(taskService.toggle("alice", 1L)).thenReturn(toggled);

        mockMvc.perform(patch("/api/tasks/{id}/toggle", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDone").value(true));
    }

    @Test
    @WithMockUser("alice")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 1L)).andExpect(status().isNoContent());

        verify(taskService).delete("alice", 1L);
    }
}
