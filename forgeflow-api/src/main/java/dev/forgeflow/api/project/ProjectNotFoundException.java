package dev.forgeflow.api.project;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class ProjectNotFoundException extends NotFoundException {
    public ProjectNotFoundException(UUID projectId) {
        super("Project not found: " + projectId);
    }
}
