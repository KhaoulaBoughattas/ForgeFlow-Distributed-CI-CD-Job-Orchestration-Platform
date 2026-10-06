package dev.forgeflow.common.event;

import java.time.Instant;

import java.util.UUID;

/**
 * Published by forgeflow-worker when a pipeline run has reached a terminal state
 * (succeeded or failed). logObjectKey points at the full log archived in object storage;
 * logTail is a short inline excerpt for quick display without a presigned download.
 */
public record PipelineFinishedEvent(
        UUID pipelineId,
        boolean succeeded,
        int exitCode,
        String logTail,
        String logObjectKey,
        Instant finishedAt
) {
}
