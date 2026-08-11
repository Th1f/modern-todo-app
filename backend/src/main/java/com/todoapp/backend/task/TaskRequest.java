package com.todoapp.backend.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        boolean isDone) {}
