package dev.forgeflow.api.common.web;

/** Base type for "the requested resource does not exist" domain errors. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
