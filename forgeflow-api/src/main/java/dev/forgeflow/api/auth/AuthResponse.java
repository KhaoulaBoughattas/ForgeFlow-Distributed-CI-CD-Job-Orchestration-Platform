package dev.forgeflow.api.auth;

import dev.forgeflow.api.user.UserSummary;

public record AuthResponse(String token, UserSummary user) {
}
