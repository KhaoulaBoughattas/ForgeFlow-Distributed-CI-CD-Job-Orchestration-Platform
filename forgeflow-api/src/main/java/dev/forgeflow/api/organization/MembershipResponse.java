package dev.forgeflow.api.organization;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(UUID userId, String email, String displayName, MembershipRole role, Instant since) {
}
