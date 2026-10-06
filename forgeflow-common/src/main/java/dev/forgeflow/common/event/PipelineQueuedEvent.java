package dev.forgeflow.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by forgeflow-api, after transaction commit, when a pipeline run has been
 * admitted and is ready for a worker to pick up. Consumed by forgeflow-worker.
 */
public record PipelineQueuedEvent(
        UUID pipelineId,
        UUID repositoryId,
        String githubOwner,
        String githubRepo,
        String commitSha,
        String branch,
        Instant occurredAt
) {
}
