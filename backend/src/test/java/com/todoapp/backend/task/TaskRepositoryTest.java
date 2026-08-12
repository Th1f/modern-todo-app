package com.todoapp.backend.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.user.User;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private User alice;
    private User bob;
    private Category aliceWork;
    private Category alicePersonal;
    private Category bobWork;
    private Task aliceTask;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("irrelevant-hash");
        return entityManager.persist(user);
    }

    private Category persistCategory(User owner, String name, String color) {
        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        category.setOwner(owner);
        return entityManager.persist(category);
    }

    private Task persistTask(User owner, Category category, String name) {
        Task task = new Task();
        task.setName(name);
        task.setOwner(owner);
        task.setCategory(category);
        return entityManager.persist(task);
    }

    @BeforeEach
    void seed() {
        alice = persistUser("alice");
        bob = persistUser("bob");

        aliceWork = persistCategory(alice, "Work", "#3B82F6");
        alicePersonal = persistCategory(alice, "Personal", "#10B981");
        bobWork = persistCategory(bob, "Work", "#3B82F6");

        aliceTask = persistTask(alice, aliceWork, "Alice work task");
        persistTask(alice, alicePersonal, "Alice personal task");
        persistTask(bob, bobWork, "Bob work task");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByOwnerUsernameReturnsOnlyThatOwnersTasks() {
        List<Task> tasks = taskRepository.findByOwnerUsername("alice");

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::getName)
                .containsExactlyInAnyOrder("Alice work task", "Alice personal task");
    }

    @Test
    void findByOwnerUsernameIsEmptyForAnUnknownUser() {
        assertThat(taskRepository.findByOwnerUsername("nobody")).isEmpty();
    }

    @Test
    void findByIdAndOwnerUsernameFindsTheOwnersOwnTask() {
        assertThat(taskRepository.findByIdAndOwnerUsername(aliceTask.getId(), "alice"))
                .isPresent()
                .get()
                .extracting(Task::getName)
                .isEqualTo("Alice work task");
    }

    @Test
    void findByIdAndOwnerUsernameHidesAnotherUsersTask() {
        assertThat(taskRepository.findById(aliceTask.getId())).isPresent();
        assertThat(taskRepository.findByIdAndOwnerUsername(aliceTask.getId(), "bob")).isEmpty();
    }

    @Test
    void findByOwnerAndCategoryNameMatchesCaseInsensitively() {
        assertThat(taskRepository.findByOwnerUsernameAndCategoryNameIgnoreCase("alice", "work"))
                .extracting(Task::getName)
                .containsExactly("Alice work task");
    }

    @Test
    void findByOwnerAndCategoryNameDoesNotCrossOwners() {
        assertThat(taskRepository.findByOwnerUsernameAndCategoryNameIgnoreCase("bob", "Work"))
                .extracting(Task::getName)
                .containsExactly("Bob work task");
    }

    @Test
    void countByCategoryIdCountsOnlyThatCategory() {
        assertThat(taskRepository.countByCategoryId(aliceWork.getId())).isEqualTo(1);
        assertThat(taskRepository.countByCategoryId(alicePersonal.getId())).isEqualTo(1);
        assertThat(taskRepository.countByCategoryId(bobWork.getId())).isEqualTo(1);
    }
}
