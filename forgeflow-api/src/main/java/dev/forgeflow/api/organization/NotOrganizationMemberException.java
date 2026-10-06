package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.ForbiddenException;

public class NotOrganizationMemberException extends ForbiddenException {
    public NotOrganizationMemberException() {
        super("You are not a member of this organization");
    }
}
