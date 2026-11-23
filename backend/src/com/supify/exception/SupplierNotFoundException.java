package com.supify.exception;

/**
 * Thrown when a requested supplier organisation or profile cannot be located.
 */
public class SupplierNotFoundException extends Exception {
    public SupplierNotFoundException(String message) {
        super(message);
    }

    public SupplierNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
