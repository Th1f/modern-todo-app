package com.todoapp.backend.category;

/** The API's view of a category. Decoupled from the JPA entity on purpose. */
public record CategoryResponse(Long id, String name, String color) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
}
