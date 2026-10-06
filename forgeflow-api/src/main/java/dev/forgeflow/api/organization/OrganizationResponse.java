package dev.forgeflow.api.organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, MembershipRole yourRole, Instant createdAt) {
    public static OrganizationResponse from(Organization organization, MembershipRole yourRole) {
        return new OrganizationResponse(
                organization.getId(), organization.getName(), organization.getSlug(), yourRole, organization.getCreatedAt());
    }
}
