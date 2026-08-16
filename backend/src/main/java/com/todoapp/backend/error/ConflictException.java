package com.todoapp.backend.error;

/** The request clashes with existing state, such as a duplicate name. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
