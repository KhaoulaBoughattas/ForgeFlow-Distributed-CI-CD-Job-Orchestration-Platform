package dev.forgeflow.api.organization;

import dev.forgeflow.api.common.web.ConflictException;

public class AlreadyAMemberException extends ConflictException {
    public AlreadyAMemberException() {
        super("This user is already a member of the organization");
    }
}
