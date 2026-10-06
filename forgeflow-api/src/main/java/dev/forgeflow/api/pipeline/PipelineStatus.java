package dev.forgeflow.api.pipeline;

/** Lifecycle of a single pipeline run. */
public enum PipelineStatus {
    PENDING,
    QUEUED,
    RUNNING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
