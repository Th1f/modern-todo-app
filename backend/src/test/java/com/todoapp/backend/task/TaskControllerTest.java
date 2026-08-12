package com.todoapp.backend.task;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.config.SecurityBeans;
import com.todoapp.backend.config.SecurityConfig;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

import org.springframework.http.MediaType;

@WebMvcTest(TaskController.class)
@Import({ SecurityConfig.class, SecurityBeans.class })
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskRepository taskRepository;
    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private UserRepository userRepository;

    private Task taskOwnedBy(String username, long id, String name) {
        User owner = new User();
        owner.setUsername(username);

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
    }

    @Test
    @WithMockUser("alice")
    void listReturnsOnlyCallersTasks() throws Exception {
        when(taskRepository.findByOwnerUsername("alice"))
                .thenReturn(List.of(taskOwnedBy("alice", 1L, "Alice private")));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice private"))
                .andExpect(jsonPath("$[0].isDone").exists());
    }

    @Test
    @WithMockUser("alice")
    void createCallersTask() throws Exception {
        User alice = new User();
        alice.setUsername("alice");

        Category work = new Category();
        work.setId(1L);
        work.setName("Work");
        work.setColor("#3B82F6");
        work.setOwner(alice);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(work));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Buy milk\",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Buy milk"));

    }

    @Test
    @WithMockUser("alice")
    void updateCallersTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    @WithMockUser("alice")
    void toggleCallersTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice Private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}/toggle", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDone").value(true));
    }

    @Test
    @WithMockUser("alice")
    void deleteCallersTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice Private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));

        mockMvc.perform(delete("/api/tasks/{id}", 1L)).andExpect(status().isNoContent());

        // 204 has no body, so this is the only assertion that the row was actually removed.
        verify(taskRepository).delete(task);
    }

    @Test
    @WithMockUser("john")
    void cannotUpdateAnotherUsersTask() throws Exception {
        Task aliceTask = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(aliceTask));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"stolen\"}")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("john")
    void cannotDeleteAnotherUsersTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice Private");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        mockMvc.perform(delete("/api/tasks/{id}", 1L)).andExpect(status().isNotFound());

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @WithMockUser("john")
    void listDoesNotLeakAnotherUsersTasks() throws Exception {
        Task aliceTask = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByOwnerUsername("alice")).thenReturn(List.of(aliceTask));
        when(taskRepository.findAll()).thenReturn(List.of(aliceTask));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser("alice")
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isBadRequest());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser("alice")
    void updateRejectsBlankName() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser("alice")
    void cannotFileTaskUnderAnotherUsersCategory() throws Exception {
        User alice = new User();
        alice.setUsername("alice");

        User bob = new User();
        bob.setUsername("bob");

        Category bobsCategory = new Category();
        bobsCategory.setId(99L);
        bobsCategory.setName("Bob secret");
        bobsCategory.setColor("#EF4444");
        bobsCategory.setOwner(bob);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryRepository.findById(99L)).thenReturn(Optional.of(bobsCategory));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sneaky\",\"categoryId\":99,\"isDone\":false}"))
                .andExpect(status().isBadRequest());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser("alice")
    void cannotMoveTaskIntoAnotherUsersCategory() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");

        User bob = new User();
        bob.setUsername("bob");

        Category bobsCategory = new Category();
        bobsCategory.setId(99L);
        bobsCategory.setName("Bob secret");
        bobsCategory.setColor("#EF4444");
        bobsCategory.setOwner(bob);

        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(categoryRepository.findById(99L)).thenReturn(Optional.of(bobsCategory));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":99}"))
                .andExpect(status().isBadRequest());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser("alice")
    void updateLeavesOmittedFieldsAlone() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        task.setDone(true);

        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.isDone").value(true))
                .andExpect(jsonPath("$.category.name").value("Work"));
    }

    @Test
    @WithMockUser("alice")
    void listFiltersByCategoryParam() throws Exception {
        Task work = taskOwnedBy("alice", 1L, "Alice private");

        when(taskRepository.findByOwnerUsernameAndCategoryNameIgnoreCase("alice", "Work"))
                .thenReturn(List.of(work));
        // Tripwire: the unfiltered query returns an extra task, so ignoring the
        // ?category param shows up as a length mismatch instead of passing quietly.
        when(taskRepository.findByOwnerUsername("alice"))
                .thenReturn(List.of(work, taskOwnedBy("alice", 2L, "Unfiltered")));

        mockMvc.perform(get("/api/tasks").param("category", "Work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice private"));
    }

    @Test
    @WithMockUser("alice")
    void listWithoutCategoryParamReturnsEverything() throws Exception {
        when(taskRepository.findByOwnerUsername("alice"))
                .thenReturn(List.of(
                        taskOwnedBy("alice", 1L, "Alice private"),
                        taskOwnedBy("alice", 2L, "Unfiltered")));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser("ghost")
    void createReturns401WhenTheSessionUserNoLongerExists() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Buy milk\",\"categoryId\":1,\"isDone\":false}"))
                .andExpect(status().isUnauthorized());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser("alice")
    void updateMovesTaskToAnotherOwnedCategory() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");

        Category personal = new Category();
        personal.setId(2L);
        personal.setName("Personal");
        personal.setColor("#10B981");
        personal.setOwner(task.getOwner());

        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(categoryRepository.findByIdAndOwnerUsername(2L, "alice")).thenReturn(Optional.of(personal));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(2))
                .andExpect(jsonPath("$.category.name").value("Personal"))
                .andExpect(jsonPath("$.name").value("Alice private"));
    }

    @Test
    @WithMockUser("alice")
    void updateCompletesTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isDone\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDone").value(true))
                .andExpect(jsonPath("$.name").value("Alice private"));
    }

    @Test
    @WithMockUser("alice")
    void updateUncompletesTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        task.setDone(true);
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isDone\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDone").value(false));
    }

    @Test
    @WithMockUser("alice")
    void toggleTurnsADoneTaskBackOff() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        task.setDone(true);
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(patch("/api/tasks/{id}/toggle", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDone").value(false));
    }

    @Test
    @WithMockUser("john")
    void cannotToggleAnotherUsersTask() throws Exception {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        mockMvc.perform(patch("/api/tasks/{id}/toggle", 1L))
                .andExpect(status().isNotFound());

        verify(taskRepository, never()).save(any(Task.class));
    }

}
