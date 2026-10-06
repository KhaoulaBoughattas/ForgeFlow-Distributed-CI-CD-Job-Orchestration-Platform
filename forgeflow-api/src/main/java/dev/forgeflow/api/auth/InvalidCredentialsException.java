package dev.forgeflow.api.auth;

import dev.forgeflow.api.common.web.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
