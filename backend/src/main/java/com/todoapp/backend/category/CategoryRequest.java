package com.todoapp.backend.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank
                @Pattern(
                        regexp = "^#(?:[0-9a-fA-F]{3}){1,2}$",
                        message = "must be a hex colour such as #3B82F6")
                String color) {}
