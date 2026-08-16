package com.todoapp.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.error.NotAuthenticatedException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService service;

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private Category category(User owner, long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setColor("#3B82F6");
        category.setOwner(owner);
        return category;
    }

    private Task taskOwnedBy(String username, long id, String name) {
        User owner = user(username);
        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setCategory(category(owner, 1L, "Work"));
        task.setOwner(owner);
        return task;
    }

    private void echoSavedTask() {
        when(taskRepository.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * Makes the row findable by plain id. The service must still refuse, which
     * is what fails if the owner-scoped query is ever swapped for findById.
     */
    private void makeFindableByIdAlone(Task task) {
        lenient().when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
    }

    // ---------- list ----------

    @Test
    void listUsesTheOwnerScopedQuery() {
        List<Task> expected = List.of(taskOwnedBy("alice", 1L, "Alice private"));
        when(taskRepository.findByOwnerUsername("alice")).thenReturn(expected);

        assertThat(service.listFor("alice", null)).isEqualTo(expected);
        verify(taskRepository, never()).findAll();
    }

    @Test
    void listFiltersByCategoryWhenAsked() {
        List<Task> expected = List.of(taskOwnedBy("alice", 1L, "Alice private"));
        when(taskRepository.findByOwnerUsernameAndCategoryNameIgnoreCase("alice", "Work"))
                .thenReturn(expected);

        assertThat(service.listFor("alice", "Work")).isEqualTo(expected);
        verify(taskRepository, never()).findByOwnerUsername("alice");
    }

    // ---------- create ----------

    @Test
    void createTakesTheOwnerFromTheSession() {
        User alice = user("alice");
        Category work = category(alice, 1L, "Work");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(work));
        echoSavedTask();

        Task created = service.create("alice", new TaskRequest("Buy milk", 1L, false));

        assertThat(created.getName()).isEqualTo("Buy milk");
        assertThat(created.getOwner()).isSameAs(alice);
        assertThat(created.getCategory()).isSameAs(work);
        assertThat(created.isDone()).isFalse();
    }

    @Test
    void createRejectsASessionUserThatNoLongerExists() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("ghost", new TaskRequest("Buy milk", 1L, false)))
                .isInstanceOf(NotAuthenticatedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createRefusesAnotherUsersCategory() {
        User alice = user("alice");
        Category bobs = category(user("bob"), 99L, "Bob secret");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        lenient().when(categoryRepository.findById(99L)).thenReturn(Optional.of(bobs));

        assertThatThrownBy(() -> service.create("alice", new TaskRequest("Sneaky", 99L, false)))
                .isInstanceOf(InvalidRequestException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    // ---------- update ----------

    @Test
    void updateRenamesAndTrims() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        echoSavedTask();

        Task updated = service.update("alice", 1L, new TaskUpdate("  Renamed  ", null, null));

        assertThat(updated.getName()).isEqualTo("Renamed");
    }

    @Test
    void updateRejectsABlankName() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.update("alice", 1L, new TaskUpdate("   ", null, null)))
                .isInstanceOf(InvalidRequestException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateLeavesOmittedFieldsAlone() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        task.setDone(true);
        Category original = task.getCategory();
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        echoSavedTask();

        Task updated = service.update("alice", 1L, new TaskUpdate("Renamed", null, null));

        assertThat(updated.isDone()).isTrue();
        assertThat(updated.getCategory()).isSameAs(original);
    }

    @Test
    void updateMovesTheTaskToAnotherOwnedCategory() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        Category personal = category(task.getOwner(), 2L, "Personal");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        when(categoryRepository.findByIdAndOwnerUsername(2L, "alice"))
                .thenReturn(Optional.of(personal));
        echoSavedTask();

        Task updated = service.update("alice", 1L, new TaskUpdate(null, 2L, null));

        assertThat(updated.getCategory()).isSameAs(personal);
    }

    @Test
    void updateRefusesToMoveIntoAnotherUsersCategory() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        lenient().when(categoryRepository.findById(99L))
                .thenReturn(Optional.of(category(user("bob"), 99L, "Bob secret")));

        assertThatThrownBy(() -> service.update("alice", 1L, new TaskUpdate(null, 99L, null)))
                .isInstanceOf(InvalidRequestException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateCanCompleteAndUncomplete() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        echoSavedTask();

        assertThat(service.update("alice", 1L, new TaskUpdate(null, null, true)).isDone()).isTrue();
        assertThat(service.update("alice", 1L, new TaskUpdate(null, null, false)).isDone()).isFalse();
    }

    @Test
    void updateRefusesAnotherUsersTask() {
        Task alices = taskOwnedBy("alice", 1L, "Alice private");
        makeFindableByIdAlone(alices);

        assertThatThrownBy(() -> service.update("john", 1L, new TaskUpdate("Stolen", null, null)))
                .isInstanceOf(NotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    // ---------- toggle ----------

    @Test
    void toggleFlipsInBothDirections() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));
        echoSavedTask();

        assertThat(service.toggle("alice", 1L).isDone()).isTrue();
        assertThat(service.toggle("alice", 1L).isDone()).isFalse();
    }

    @Test
    void toggleRefusesAnotherUsersTask() {
        makeFindableByIdAlone(taskOwnedBy("alice", 1L, "Alice private"));

        assertThatThrownBy(() -> service.toggle("john", 1L)).isInstanceOf(NotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesTheCallersTask() {
        Task task = taskOwnedBy("alice", 1L, "Alice private");
        when(taskRepository.findByIdAndOwnerUsername(1L, "alice")).thenReturn(Optional.of(task));

        service.delete("alice", 1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteRefusesAnotherUsersTask() {
        makeFindableByIdAlone(taskOwnedBy("alice", 1L, "Alice private"));

        assertThatThrownBy(() -> service.delete("john", 1L)).isInstanceOf(NotFoundException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }
}
