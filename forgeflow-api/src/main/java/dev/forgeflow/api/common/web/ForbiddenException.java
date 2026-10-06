package dev.forgeflow.api.common.web;

/** Base type for "you are authenticated but not allowed to do this" domain errors. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
