package com.supify.exception;

/**
 * Thrown when a claim verification step, state transition, or attestation check fails.
 */
public class VerificationException extends Exception {
    public VerificationException(String message) {
        super(message);
    }

    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
