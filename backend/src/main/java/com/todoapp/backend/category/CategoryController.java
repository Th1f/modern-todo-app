package com.todoapp.backend.category;

import com.todoapp.backend.task.TaskRepository;
import com.todoapp.backend.user.User;
import com.todoapp.backend.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CategoryController(
            CategoryRepository repository,
            TaskRepository taskRepository,
            UserRepository userRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Category> list(Authentication auth) {
        return repository.findByOwnerUsernameOrderByNameAsc(auth.getName());
    }

    @PostMapping
    public ResponseEntity<Category> create(
            @Valid @RequestBody Category category, Authentication auth) {
        
        if (repository.existsByOwnerUsernameAndNameIgnoreCase(auth.getName(), category.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User owner = userRepository.findByUsername(auth.getName()).orElse(null);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        category.setId(null);
        category.setOwner(owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(category));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Category> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdate update,
            Authentication auth) {

        Category category = repository.findByIdAndOwnerUsername(id, auth.getName()).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        if (update.name() != null) {
            String name = update.name().trim();
            if (name.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // Renaming to a different existing name collides; renaming to your
            // own name (or a case variant of it) is fine.
            if (!name.equalsIgnoreCase(category.getName())
                    && repository.existsByOwnerUsernameAndNameIgnoreCase(auth.getName(), name)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            category.setName(name);
        }

        if (update.color() != null) {
            category.setColor(update.color());
        }

        return ResponseEntity.ok(repository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Category category = repository.findByIdAndOwnerUsername(id, auth.getName()).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        // Deleting a category that still owns tasks would violate the foreign
        // key; refuse it explicitly instead of letting the database error out.
        if (taskRepository.countByCategoryId(id) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        repository.delete(category);
        return ResponseEntity.noContent().build();
    }
}
