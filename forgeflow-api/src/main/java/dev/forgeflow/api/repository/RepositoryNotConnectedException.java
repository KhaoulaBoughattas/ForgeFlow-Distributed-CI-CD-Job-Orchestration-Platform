package dev.forgeflow.api.repository;

import dev.forgeflow.api.common.web.ConflictException;

public class RepositoryNotConnectedException extends ConflictException {
    public RepositoryNotConnectedException(String githubOwner, String githubRepo) {
        super("Repository " + githubOwner + "/" + githubRepo + " is not connected to any project");
    }
}
