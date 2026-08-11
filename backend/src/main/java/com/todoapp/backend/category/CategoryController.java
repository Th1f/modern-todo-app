package com.todoapp.backend.category;

import com.todoapp.backend.task.TaskRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public CategoryController(CategoryRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<Category> list() {
        return repository.findAll(Sort.by("name"));
    }

    @PostMapping
    public ResponseEntity<Category> create(@Valid @RequestBody Category category) {
        if (repository.existsByNameIgnoreCase(category.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        category.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(category));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Category> update(
            @PathVariable Long id, @Valid @RequestBody CategoryUpdate update) {
        Category category = repository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        if (update.name() != null) {
            String name = update.name().trim();
            if (name.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!name.equalsIgnoreCase(category.getName())
                    && repository.existsByNameIgnoreCase(name)) {
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (taskRepository.countByCategoryId(id) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
