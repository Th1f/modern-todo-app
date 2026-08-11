package com.todoapp.backend.task;

import com.todoapp.backend.category.CategoryRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repository;
    private final CategoryRepository categoryRepository;

    public TaskController(TaskRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Task> list(@RequestParam(required = false) String category) {
        return category == null
                ? repository.findAll()
                : repository.findByCategoryNameIgnoreCase(category);
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest request) {
        return categoryRepository
                .findById(request.categoryId())
                .map(category -> {
                    Task task = new Task();
                    task.setName(request.name());
                    task.setCategory(category);
                    task.setDone(request.isDone());
                    return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(task));
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> update(
            @PathVariable Long id, @RequestBody TaskUpdate update) {
        Task task = repository.findById(id).orElse(null);
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
            var category = categoryRepository.findById(update.categoryId()).orElse(null);
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
    public ResponseEntity<Task> toggleDone(@PathVariable Long id) {
        return repository.findById(id)
                .map(task -> {
                    task.setDone(!task.isDone());
                    return ResponseEntity.ok(repository.save(task));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
