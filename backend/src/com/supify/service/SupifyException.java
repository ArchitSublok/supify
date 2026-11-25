package com.supify.service;

// Custom checked exception for Supify validation and lookup errors
public class SupifyException extends Exception {
    public SupifyException(String message) {
        super(message);
    }

    public SupifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
