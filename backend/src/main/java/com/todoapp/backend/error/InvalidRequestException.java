package com.todoapp.backend.error;

/** A rule the bean-validation annotations cannot express was broken. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
