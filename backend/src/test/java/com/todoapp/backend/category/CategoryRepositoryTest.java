package com.todoapp.backend.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.hibernate.exception.ConstraintViolationException;

import com.todoapp.backend.user.User;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User alice;
    private User bob;
    private Category aliceWork;

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

    @BeforeEach
    void seed() {
        alice = persistUser("alice");
        bob = persistUser("bob");

        aliceWork = persistCategory(alice, "Work", "#3B82F6");
        persistCategory(alice, "Errands", "#F59E0B");
        persistCategory(bob, "Work", "#3B82F6");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByOwnerReturnsOnlyThatOwnersCategoriesSortedByName() {
        List<Category> categories = categoryRepository.findByOwnerUsernameOrderByNameAsc("alice");

        assertThat(categories).extracting(Category::getName).containsExactly("Errands", "Work");
    }

    @Test
    void findByIdAndOwnerUsernameHidesAnotherUsersCategory() {
        assertThat(categoryRepository.findById(aliceWork.getId())).isPresent();
        assertThat(categoryRepository.findByIdAndOwnerUsername(aliceWork.getId(), "alice")).isPresent();
        assertThat(categoryRepository.findByIdAndOwnerUsername(aliceWork.getId(), "bob")).isEmpty();
    }

    @Test
    void existsByOwnerAndNameIsCaseInsensitiveAndOwnerScoped() {
        assertThat(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "work")).isTrue();
        assertThat(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "WORK")).isTrue();
        assertThat(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("alice", "Fitness")).isFalse();
        assertThat(categoryRepository.existsByOwnerUsernameAndNameIgnoreCase("bob", "Errands")).isFalse();
    }

    @Test
    void existsByOwnerIdDistinguishesUsers() {
        assertThat(categoryRepository.existsByOwnerId(alice.getId())).isTrue();
        assertThat(categoryRepository.existsByOwnerId(bob.getId())).isTrue();
    }

    @Test
    void twoUsersMayEachOwnACategoryOfTheSameName() {
        assertThat(categoryRepository.findByOwnerUsernameOrderByNameAsc("alice"))
                .extracting(Category::getName)
                .contains("Work");
        assertThat(categoryRepository.findByOwnerUsernameOrderByNameAsc("bob"))
                .extracting(Category::getName)
                .contains("Work");
    }

    @Test
    void oneUserMayNotOwnTwoCategoriesOfTheSameName() {
        assertThatThrownBy(() -> {
            persistCategory(alice, "Work", "#000000");
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
