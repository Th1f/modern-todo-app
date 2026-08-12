package com.todoapp.backend.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;
import com.todoapp.backend.task.TaskRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

@WebMvcTest(CategoryController.class)
@Import({ SecurityConfig.class, SecurityBeans.class })
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private TaskRepository taskRepository;
    @MockitoBean
    private UserRepository userRepository;

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private Category categoryOwnedBy(String username, long id, String name, String color) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setColor(color);
        category.setOwner(user(username));
        return category;
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    // ---------- list ----------

    @Test
    @WithMockUser("alice")
    void listReturnsOnlyCallersCategories() throws Exception {
        when(categoryRepository.findByOwnerUsernameOrderByNameAsc("alice"))
                .thenReturn(List.of(
                        categoryOwnedBy("alice", 1L, "Errands", "#F59E0B"),
                        categoryOwnedBy("alice", 2L, "Work", "#3B82F6")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Errands"))
                .andExpect(jsonPath("$[1].name").value("Work"))
                .andExpect(jsonPath("$[0].owner").doesNotExist());
    }

    @Test
    @WithMockUser("john")
    void listDoesNotLeakAnotherUsersCategories() throws Exception {
        Category alices = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByOwnerUsernameOrderByNameAsc("alice")).thenReturn(List.of(alices));
        when(categoryRepository.findAll()).thenReturn(List.of(alices));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- create ----------

    @Test
    @WithMockUser("alice")
    void createCallersCategory() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Fitness\",\"color\":\"#EF4444\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fitness"))
                .andExpect(jsonPath("$.color").value("#EF4444"));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getOwner().getUsername()).isEqualTo("alice");
    }

    @Test
    @WithMockUser("alice")
    void createIgnoresClientSuppliedId() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":99,\"name\":\"Fitness\",\"color\":\"#EF4444\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
    }

    @Test
    @WithMockUser("alice")
    void createRejectsDuplicateName() throws Exception {
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Work"))
                .thenReturn(true);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Work\",\"color\":\"#3B82F6\"}"))
                .andExpect(status().isConflict());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("alice")
    void createAllowsANameAnotherUserAlreadyUses() throws Exception {
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("bob", "Work")).thenReturn(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Work\",\"color\":\"#3B82F6\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser("ghost")
    void createReturns401WhenTheSessionUserNoLongerExists() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Fitness\",\"color\":\"#EF4444\"}"))
                .andExpect(status().isUnauthorized());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("alice")
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"color\":\"#3B82F6\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("alice")
    void createRejectsNonHexColour() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Fitness\",\"color\":\"red\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ---------- update ----------

    @Test
    @WithMockUser("alice")
    void updateRenamesCallersCategory() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Job\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Job"))
                .andExpect(jsonPath("$.color").value("#3B82F6"));
    }

    @Test
    @WithMockUser("alice")
    void updateChangesColourAlone() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"#10B981\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.color").value("#10B981"));
    }

    @Test
    @WithMockUser("alice")
    void updateAllowsRecasingItsOwnName() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "work")).thenReturn(true);

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("work"));
    }

    @Test
    @WithMockUser("alice")
    void updateRejectsRenameOntoAnotherCategory() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Personal")).thenReturn(true);

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Personal\"}"))
                .andExpect(status().isConflict());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("alice")
    void updateRejectsBlankName() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("alice")
    void updateRejectsNonHexColour() throws Exception {
        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"not-a-colour\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @WithMockUser("john")
    void cannotUpdateAnotherUsersCategory() throws Exception {
        Category alices = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(alices));

        mockMvc.perform(patch("/api/categories/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isNotFound());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ---------- delete ----------

    @Test
    @WithMockUser("alice")
    void deleteEmptyCategory() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(taskRepository.countByCategoryId(1L)).thenReturn(0L);

        mockMvc.perform(delete("/api/categories/{id}", 1L)).andExpect(status().isNoContent());

        verify(categoryRepository).delete(category);
    }

    @Test
    @WithMockUser("alice")
    void deleteRejectsCategoryThatStillHasTasks() throws Exception {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(category));
        when(taskRepository.countByCategoryId(1L)).thenReturn(3L);

        mockMvc.perform(delete("/api/categories/{id}", 1L)).andExpect(status().isConflict());

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @WithMockUser("john")
    void cannotDeleteAnotherUsersCategory() throws Exception {
        Category alices = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(alices));

        mockMvc.perform(delete("/api/categories/{id}", 1L)).andExpect(status().isNotFound());

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
