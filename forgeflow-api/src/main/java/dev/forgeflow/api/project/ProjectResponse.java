package dev.forgeflow.api.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, UUID organizationId, String name, String slug, Instant createdAt) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getOrganizationId(), project.getName(), project.getSlug(), project.getCreatedAt());
    }
}
