package dev.forgeflow.api.project;

import dev.forgeflow.api.common.web.ConflictException;

public class SlugAlreadyExistsInOrganizationException extends ConflictException {
    public SlugAlreadyExistsInOrganizationException(String slug) {
        super("A project with slug '" + slug + "' already exists in this organization");
    }
}
