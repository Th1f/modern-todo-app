package com.todoapp.backend.category;

import jakarta.validation.constraints.Pattern;

public record CategoryUpdate(
        String name,
        @Pattern(
                        regexp = "^#(?:[0-9a-fA-F]{3}){1,2}$",
                        message = "must be a hex colour such as #3B82F6")
                String color) {}
