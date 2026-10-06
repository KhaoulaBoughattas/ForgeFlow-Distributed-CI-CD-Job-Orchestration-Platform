package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.ConflictException;

public class SlugAlreadyExistsException extends ConflictException {
    public SlugAlreadyExistsException(String slug) {
        super("An organization with slug '" + slug + "' already exists");
    }
}
