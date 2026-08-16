package com.todoapp.backend.task;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.error.InvalidRequestException;
import com.todoapp.backend.error.NotAuthenticatedException;
import com.todoapp.backend.error.NotFoundException;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final CategoryRepository categories;
    private final UserRepository users;

    public TaskService(
            TaskRepository tasks, CategoryRepository categories, UserRepository users) {
        this.tasks = tasks;
        this.categories = categories;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Task> listFor(String username, String categoryName) {
        return categoryName == null
                ? tasks.findByOwnerUsername(username)
                : tasks.findByOwnerUsernameAndCategoryNameIgnoreCase(username, categoryName);
    }

    public Task create(String username, TaskRequest request) {
        User owner = users.findByUsername(username)
                .orElseThrow(() -> new NotAuthenticatedException(username));

        Task task = new Task();
        task.setName(request.name());
        task.setCategory(ownedCategory(request.categoryId(), username));
        task.setDone(request.isDone());
        task.setOwner(owner);
        return tasks.save(task);
    }

    public Task update(String username, Long id, TaskUpdate update) {
        Task task = ownedTask(id, username);

        if (update.name() != null) {
            task.rename(update.name());
        }
        if (update.categoryId() != null) {
            task.setCategory(ownedCategory(update.categoryId(), username));
        }
        if (update.isDone() != null) {
            task.setDone(update.isDone());
        }
        return tasks.save(task);
    }

    public Task toggle(String username, Long id) {
        Task task = ownedTask(id, username);
        task.toggle();
        return tasks.save(task);
    }

    public void delete(String username, Long id) {
        tasks.delete(ownedTask(id, username));
    }

    /**
     * The one place a task is fetched by id. Scoping the query by owner means
     * no caller can forget the check.
     */
    private Task ownedTask(Long id, String username) {
        return tasks.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundException("Task " + id));
    }

    /** Filing a task under someone else's category is a bad request, not a 404. */
    private Category ownedCategory(Long id, String username) {
        return categories.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new InvalidRequestException("Category " + id + " is not yours"));
    }
}
