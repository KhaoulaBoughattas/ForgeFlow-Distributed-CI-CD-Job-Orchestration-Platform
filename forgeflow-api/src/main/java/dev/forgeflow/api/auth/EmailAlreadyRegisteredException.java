package dev.forgeflow.api.auth;

import dev.forgeflow.api.common.web.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {
    public EmailAlreadyRegisteredException(String email) {
        super("An account already exists for email: " + email);
    }
}
