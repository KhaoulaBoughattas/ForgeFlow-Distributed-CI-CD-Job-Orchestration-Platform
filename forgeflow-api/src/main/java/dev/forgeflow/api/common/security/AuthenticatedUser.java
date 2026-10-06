package dev.forgeflow.api.common.security;

import java.util.UUID;

/** Minimal principal placed into the SecurityContext by {@link JwtAuthenticationFilter}. */
public record AuthenticatedUser(UUID id, String email) {
}
