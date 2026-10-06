package dev.forgeflow.api.user;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }

    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException forEmail(String email) {
        return new UserNotFoundException("User not found for email: " + email);
    }
}
