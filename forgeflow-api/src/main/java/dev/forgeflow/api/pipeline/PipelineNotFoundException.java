package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class PipelineNotFoundException extends NotFoundException {
    public PipelineNotFoundException(UUID pipelineId) {
        super("Pipeline not found: " + pipelineId);
    }
}
