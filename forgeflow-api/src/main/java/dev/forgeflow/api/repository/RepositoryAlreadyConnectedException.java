package dev.forgeflow.api.repository;

import dev.forgeflow.api.common.web.ConflictException;

public class RepositoryAlreadyConnectedException extends ConflictException {
    public RepositoryAlreadyConnectedException(String githubOwner, String githubRepo) {
        super("Repository " + githubOwner + "/" + githubRepo + " is already connected to this project");
    }
}
