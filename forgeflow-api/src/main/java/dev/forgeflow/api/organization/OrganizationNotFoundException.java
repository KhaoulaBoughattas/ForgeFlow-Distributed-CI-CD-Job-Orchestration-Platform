package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.NotFoundException;

import java.util.UUID;

public class OrganizationNotFoundException extends NotFoundException {
    public OrganizationNotFoundException(UUID organizationId) {
        super("Organization not found: " + organizationId);
    }
}
