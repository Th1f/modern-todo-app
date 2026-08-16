package com.todoapp.backend.category;

import com.todoapp.backend.error.ConflictException;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.error.NotAuthenticatedException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.task.TaskRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;
    private final TaskRepository tasks;
    private final UserRepository users;

    public CategoryService(
            CategoryRepository categories, TaskRepository tasks, UserRepository users) {
        this.categories = categories;
        this.tasks = tasks;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Category> listFor(String username) {
        return categories.findByOwnerUsernameOrderByNameAsc(username);
    }

    /**
     * Takes the fields rather than a Category, so a client-supplied id cannot
     * reach the entity at all.
     */
    public Category create(String username, String name, String color) {
        if (categories.existsByOwnerUsernameAndNameIgnoreCase(username, name)) {
            throw new ConflictException("\"" + name + "\" already exists");
        }
        User owner = users.findByUsername(username)
                .orElseThrow(() -> new NotAuthenticatedException(username));

        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        category.setOwner(owner);
        return categories.save(category);
    }

    public Category update(String username, Long id, CategoryUpdate update) {
        Category category = ownedCategory(id, username);

        if (update.name() != null) {
            String name = update.name().trim();
            if (name.isEmpty()) {
                throw new InvalidRequestException("Category name must not be blank");
            }
            // Renaming to a different existing name collides; renaming to your
            // own name (or a case variant of it) is fine.
            if (!name.equalsIgnoreCase(category.getName())
                    && categories.existsByOwnerUsernameAndNameIgnoreCase(username, name)) {
                throw new ConflictException("\"" + name + "\" already exists");
            }
            category.setName(name);
        }

        if (update.color() != null) {
            category.setColor(update.color());
        }

        return categories.save(category);
    }

    public void delete(String username, Long id) {
        Category category = ownedCategory(id, username);
        // Deleting a category that still owns tasks would violate the foreign
        // key; refuse it explicitly instead of letting the database error out.
        if (tasks.countByCategoryId(id) > 0) {
            throw new ConflictException("\"" + category.getName() + "\" is still in use");
        }
        categories.delete(category);
    }

    private Category ownedCategory(Long id, String username) {
        return categories.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundException("Category " + id));
    }
}
