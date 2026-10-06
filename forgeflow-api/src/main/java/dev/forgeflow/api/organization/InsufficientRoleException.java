package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.ForbiddenException;

public class InsufficientRoleException extends ForbiddenException {
    public InsufficientRoleException(MembershipRole required) {
        super("This action requires at least the " + required + " role");
    }
}
