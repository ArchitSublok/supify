package com.supify.exception;

public class SupifyException extends Exception {
    public SupifyException(String message) {
        super(message);
    }

    public SupifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
