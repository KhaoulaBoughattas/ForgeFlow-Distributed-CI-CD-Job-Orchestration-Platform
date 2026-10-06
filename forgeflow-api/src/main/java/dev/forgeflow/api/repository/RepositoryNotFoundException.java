package dev.forgeflow.api.repository;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class RepositoryNotFoundException extends NotFoundException {
    public RepositoryNotFoundException(UUID repositoryId) {
        super("Repository not found: " + repositoryId);
    }
}
