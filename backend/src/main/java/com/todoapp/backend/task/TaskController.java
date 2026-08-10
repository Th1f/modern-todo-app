package com.todoapp.backend.task;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repository;

    public TaskController(TaskRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Task> list(@RequestParam(required = false) String category) {
        return category == null
                ? repository.findAll()
                : repository.findByCategory(category);
    }

    @PostMapping
    public Task create(@Valid @RequestBody Task task) {
        task.setId(null);
        return repository.save(task);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Task> toggleDone(@PathVariable Long id) {
        return repository.findById(id)
                .map(task -> {
                    task.setDone(!task.isDone());
                    return ResponseEntity.ok(repository.save(task));
                })
                .orElse(ResponseEntity.notFound().build());
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
