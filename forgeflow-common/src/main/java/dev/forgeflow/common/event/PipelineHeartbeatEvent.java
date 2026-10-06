package dev.forgeflow.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published periodically by forgeflow-worker while a job is running, so forgeflow-api
 * can detect stalled/crashed workers (see PipelineReaper) via heartbeat staleness.
 */
public record PipelineHeartbeatEvent(
        UUID pipelineId,
        String workerId,
        Instant at
) {
}
