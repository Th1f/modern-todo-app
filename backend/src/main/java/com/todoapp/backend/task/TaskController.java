package com.todoapp.backend.task;

import com.todoapp.backend.category.Category;
import com.todoapp.backend.category.CategoryRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TaskController(
            TaskRepository repository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Task> list(@RequestParam(required = false) String category, Authentication auth) {
        return category == null
                ? repository.findByOwnerUsername(auth.getName())
                : repository.findByOwnerUsernameAndCategoryNameIgnoreCase(auth.getName(), category);
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        User owner = userRepository.findByUsername(auth.getName()).orElse(null);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Category category =
                categoryRepository.findByIdAndOwnerUsername(request.categoryId(), auth.getName())
                        .orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }

        Task task = new Task();
        task.setName(request.name());
        task.setCategory(category);
        task.setDone(request.isDone());
        task.setOwner(owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(task));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> update(
            @PathVariable Long id, @RequestBody TaskUpdate update, Authentication auth) {

        Task task = repository.findByIdAndOwnerUsername(id, auth.getName()).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        if (update.name() != null) {
            String name = update.name().trim();
            if (name.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            task.setName(name);
        }

        if (update.categoryId() != null) {
            Category category =
                    categoryRepository.findByIdAndOwnerUsername(update.categoryId(), auth.getName())
                            .orElse(null);
            if (category == null) {
                return ResponseEntity.badRequest().build();
            }
            task.setCategory(category);
        }

        if (update.isDone() != null) {
            task.setDone(update.isDone());
        }

        return ResponseEntity.ok(repository.save(task));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Task> toggleDone(@PathVariable Long id, Authentication auth) {
        return repository.findByIdAndOwnerUsername(id, auth.getName())
                .map(task -> {
                    task.setDone(!task.isDone());
                    return ResponseEntity.ok(repository.save(task));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        return repository.findByIdAndOwnerUsername(id, auth.getName())
                .map(task -> {
                    repository.delete(task);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
