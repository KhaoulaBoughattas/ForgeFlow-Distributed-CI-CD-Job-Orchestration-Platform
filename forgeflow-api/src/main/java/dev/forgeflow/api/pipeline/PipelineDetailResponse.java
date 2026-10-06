package dev.forgeflow.api.pipeline;

import java.time.Instant;
import java.util.UUID;

public record PipelineDetailResponse(
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
        String logTail,
        boolean fullLogAvailable,
        int retryCount,
        Instant createdAt
) {
    public static PipelineDetailResponse from(Pipeline pipeline) {
        return new PipelineDetailResponse(
                pipeline.getId(), pipeline.getRepositoryId(), pipeline.getGithubOwner(), pipeline.getGithubRepo(),
                pipeline.getCommitSha(), pipeline.getBranch(), pipeline.getStatus(), pipeline.getWorkerId(),
                pipeline.getStartedAt(), pipeline.getFinishedAt(), pipeline.getExitCode(), pipeline.getLogTail(),
                pipeline.getLogObjectKey() != null, pipeline.getRetryCount(), pipeline.getCreatedAt());
    }
}
