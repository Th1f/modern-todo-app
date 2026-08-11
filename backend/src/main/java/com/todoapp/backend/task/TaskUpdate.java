package com.todoapp.backend.task;

public record TaskUpdate(String name, Long categoryId, Boolean isDone) {}
