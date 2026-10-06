package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.ConflictException;

/** Thrown when an action (removing a member, demoting a role) would leave the organization with no OWNER. */
public class LastOwnerException extends ConflictException {
    public LastOwnerException() {
        super("An organization must always have at least one OWNER");
    }
}
