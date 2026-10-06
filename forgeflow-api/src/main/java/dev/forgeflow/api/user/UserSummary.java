package dev.forgeflow.api.user;

import java.time.Instant;
import java.util.UUID;

/** Public-facing projection of a {@link User}; never exposes passwordHash. */
public record UserSummary(UUID id, String email, String displayName, Instant createdAt) {
    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
