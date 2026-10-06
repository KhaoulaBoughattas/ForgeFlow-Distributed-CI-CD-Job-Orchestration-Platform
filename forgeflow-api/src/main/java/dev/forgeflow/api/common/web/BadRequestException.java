package dev.forgeflow.api.common.web;

/** Base type for malformed/invalid request errors that aren't covered by bean validation. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
