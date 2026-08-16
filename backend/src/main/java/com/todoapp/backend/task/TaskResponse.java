package com.todoapp.backend.task;

import com.todoapp.backend.category.CategoryResponse;

/**
 * The API's view of a task. The owner is absent by construction rather than by
 * remembering to annotate it away.
 */
public record TaskResponse(Long id, String name, boolean isDone, CategoryResponse category) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.isDone(),
                CategoryResponse.from(task.getCategory()));
    }
}
