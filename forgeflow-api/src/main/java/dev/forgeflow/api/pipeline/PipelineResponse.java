package dev.forgeflow.api.pipeline;

import java.time.Instant;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        UUID repositoryId,
        String githubOwner,
        String githubRepo,
        String commitSha,
        String branch,
        PipelineStatus status,
        String workerId,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        int retryCount,
        Instant createdAt
) {
    public static PipelineResponse from(Pipeline pipeline) {
        return new PipelineResponse(
                pipeline.getId(), pipeline.getRepositoryId(), pipeline.getGithubOwner(), pipeline.getGithubRepo(),
                pipeline.getCommitSha(), pipeline.getBranch(), pipeline.getStatus(), pipeline.getWorkerId(),
                pipeline.getStartedAt(), pipeline.getFinishedAt(), pipeline.getExitCode(), pipeline.getRetryCount(),
                pipeline.getCreatedAt());
    }
}
