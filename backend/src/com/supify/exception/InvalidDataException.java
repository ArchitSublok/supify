package com.supify.exception;

/**
 * Thrown when supplier, claim, or CSV import data fails validation.
 */
public class InvalidDataException extends Exception {
    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
