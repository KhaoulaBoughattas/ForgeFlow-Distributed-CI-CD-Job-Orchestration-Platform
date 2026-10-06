package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class LogNotAvailableException extends NotFoundException {
    public LogNotAvailableException(UUID pipelineId) {
        super("No full log archive is available yet for pipeline: " + pipelineId);
    }
}
