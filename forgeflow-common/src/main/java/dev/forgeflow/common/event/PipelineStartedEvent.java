package dev.forgeflow.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by forgeflow-worker as soon as it has claimed a pipeline and begun execution.
 */
public record PipelineStartedEvent(
        UUID pipelineId,
        String workerId,
        Instant startedAt
) {
}
