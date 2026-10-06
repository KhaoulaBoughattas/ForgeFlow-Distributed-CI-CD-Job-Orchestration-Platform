package dev.forgeflow.api.common.web;

/** Base type for "you are not authenticated, or your credentials/token are invalid" errors. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
