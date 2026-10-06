package dev.forgeflow.api.repository;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id, UUID projectId, String githubOwner, String githubRepo, boolean connected, Instant createdAt) {
    public static RepositoryResponse from(GitRepository repository) {
        return new RepositoryResponse(
                repository.getId(), repository.getProjectId(), repository.getGithubOwner(),
                repository.getGithubRepo(), repository.isConnected(), repository.getCreatedAt());
    }
}
