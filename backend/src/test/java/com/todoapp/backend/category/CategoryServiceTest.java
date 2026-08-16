package com.todoapp.backend.category;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todoapp.backend.error.ConflictException;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.error.NotAuthenticatedException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.task.TaskRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService service;

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

    private void echoSavedCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void makeFindableByIdAlone(Category category) {
        lenient().when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
    }

    // ---------- list ----------

    @Test
    void listUsesTheOwnerScopedQuery() {
        List<Category> expected = List.of(categoryOwnedBy("alice", 1L, "Work", "#3B82F6"));
        when(categoryRepository.findByOwnerUsernameOrderByNameAsc("alice")).thenReturn(expected);

        assertThat(service.listFor("alice")).isEqualTo(expected);
        verify(categoryRepository, never()).findAll();
    }

    // ---------- create ----------

    @Test
    void createAttachesTheSessionOwner() {
        User alice = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        echoSavedCategory();

        Category created = service.create("alice", "Fitness", "#EF4444");

        assertThat(created.getName()).isEqualTo("Fitness");
        assertThat(created.getColor()).isEqualTo("#EF4444");
        assertThat(created.getOwner()).isSameAs(alice);
    }

    @Test
    void createNeverCarriesAnIdFromTheCaller() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));
        echoSavedCategory();

        service.create("alice", "Fitness", "#EF4444");

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
    }

    @Test
    void createRejectsADuplicateNameForTheSameUser() {
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Work"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create("alice", "Work", "#3B82F6"))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createAllowsANameAnotherUserAlreadyUses() {
        lenient().when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("bob", "Work"))
                .thenReturn(true);
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Work"))
                .thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user("alice")));
        echoSavedCategory();

        assertThat(service.create("alice", "Work", "#3B82F6").getName()).isEqualTo("Work");
    }

    @Test
    void createRejectsASessionUserThatNoLongerExists() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("ghost", "Fitness", "#EF4444"))
                .isInstanceOf(NotAuthenticatedException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ---------- update ----------

    @Test
    void updateRenames() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        echoSavedCategory();

        Category updated = service.update("alice", 1L, new CategoryUpdate("Job", null));

        assertThat(updated.getName()).isEqualTo("Job");
        assertThat(updated.getColor()).isEqualTo("#3B82F6");
    }

    @Test
    void updateChangesColourAlone() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        echoSavedCategory();

        Category updated = service.update("alice", 1L, new CategoryUpdate(null, "#10B981"));

        assertThat(updated.getName()).isEqualTo("Work");
        assertThat(updated.getColor()).isEqualTo("#10B981");
    }

    @Test
    void updateAllowsRecasingItsOwnName() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        lenient().when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "work"))
                .thenReturn(true);
        echoSavedCategory();

        assertThat(service.update("alice", 1L, new CategoryUpdate("work", null)).getName())
                .isEqualTo("work");
    }

    @Test
    void updateRejectsRenamingOntoAnotherCategory() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Personal"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update("alice", 1L, new CategoryUpdate("Personal", null)))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateRejectsABlankName() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.update("alice", 1L, new CategoryUpdate("   ", null)))
                .isInstanceOf(InvalidRequestException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateRefusesAnotherUsersCategory() {
        makeFindableByIdAlone(categoryOwnedBy("alice", 1L, "Work", "#3B82F6"));

        assertThatThrownBy(() -> service.update("john", 1L, new CategoryUpdate("Stolen", null)))
                .isInstanceOf(NotFoundException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesAnEmptyCategory() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        when(taskRepository.countByCategoryId(1L)).thenReturn(0L);

        service.delete("alice", 1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteRefusesWhileTasksRemain() {
        Category category = categoryOwnedBy("alice", 1L, "Work", "#3B82F6");
        when(categoryRepository.findByIdAndOwnerUsername(1L, "alice"))
                .thenReturn(Optional.of(category));
        when(taskRepository.countByCategoryId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete("alice", 1L))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteRefusesAnotherUsersCategory() {
        makeFindableByIdAlone(categoryOwnedBy("alice", 1L, "Work", "#3B82F6"));

        assertThatThrownBy(() -> service.delete("john", 1L)).isInstanceOf(NotFoundException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
