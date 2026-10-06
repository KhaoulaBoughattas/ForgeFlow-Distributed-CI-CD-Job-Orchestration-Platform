package dev.forgeflow.api.pipeline.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal Spring application event raised (not yet published to Kafka) the moment a Pipeline
 * row is created. {@link PipelineEventPublisher} listens AFTER_COMMIT and only then publishes
 * the corresponding Kafka message, so a worker never sees a pipeline that a rolled-back
 * transaction made disappear.
 */
public record PipelineCreatedEvent(
        UUID pipelineId,
        UUID repositoryId,
        String githubOwner,
        String githubRepo,
        String commitSha,
        String branch,
        Instant occurredAt
) {
}
