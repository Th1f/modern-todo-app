package com.todoapp.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank String username,
        @NotBlank @Size(min = 8, message = "must be atleast 8 characters") String password) {
}
