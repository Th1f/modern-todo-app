package com.todoapp.backend.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.todoapp.backend.user.User;

@DataJpaTest
class CategoryDefaultsTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("irrelevant-hash");
        return entityManager.persist(user);
    }

    @Test
    void createForGivesANewUserTheThreeStarterCategories() {
        User alice = persistUser("alice");

        List<Category> created = new CategoryDefaults(categoryRepository).createFor(alice);

        assertThat(created)
                .extracting(Category::getName, Category::getColor)
                .containsExactly(
                        tuple("Work", "#3B82F6"),
                        tuple("Personal", "#10B981"),
                        tuple("Errands", "#F59E0B"));

        assertThat(created).allSatisfy(category -> {
            assertThat(category.getId()).isNotNull();
            assertThat(category.getOwner().getUsername()).isEqualTo("alice");
        });
    }

    @Test
    void createForIsScopedToTheGivenUser() {
        User alice = persistUser("alice");
        User bob = persistUser("bob");

        CategoryDefaults defaults = new CategoryDefaults(categoryRepository);
        defaults.createFor(alice);
        defaults.createFor(bob);

        entityManager.flush();

        assertThat(categoryRepository.findByOwnerUsernameOrderByNameAsc("alice")).hasSize(3);
        assertThat(categoryRepository.findByOwnerUsernameOrderByNameAsc("bob")).hasSize(3);
    }
}
