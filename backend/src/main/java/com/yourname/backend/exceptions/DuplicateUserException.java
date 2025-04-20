// src/main/java/com/yourname/backend/exceptions/DuplicateUserException.java
package com.yourname.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when attempting to register a user
 * with an email address that already exists in the system.
 *
 * Annotated with @ResponseStatus to automatically map this exception
 * to an HTTP 400 Bad Request response when thrown from a controller.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DuplicateUserException extends RuntimeException {

    /**
     * Constructs a new DuplicateUserException with the specified detail message.
     *
     * @param message the detail message.
     */
    public DuplicateUserException(String message) {
        super(message); // Pass the message to the parent RuntimeException class
    }
}