package dev.forgeflow.api.common.web;

/** Base type for "this would conflict with existing state" domain errors. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
