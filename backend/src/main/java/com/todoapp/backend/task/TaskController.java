package com.todoapp.backend.task;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(required = false) String category, Authentication auth) {
        return service.listFor(auth.getName(), category).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        return TaskResponse.from(service.create(auth.getName(), request));
    }

    @PatchMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id, @RequestBody TaskUpdate update, Authentication auth) {
        return TaskResponse.from(service.update(auth.getName(), id, update));
    }

    @PatchMapping("/{id}/toggle")
    public TaskResponse toggleDone(@PathVariable Long id, Authentication auth) {
        return TaskResponse.from(service.toggle(auth.getName(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(auth.getName(), id);
    }
}
