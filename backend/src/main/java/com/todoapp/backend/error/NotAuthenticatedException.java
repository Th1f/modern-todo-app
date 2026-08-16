package com.todoapp.backend.error;

/** The session names a user the database no longer has. */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
