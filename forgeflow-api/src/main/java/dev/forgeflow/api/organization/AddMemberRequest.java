package dev.forgeflow.api.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank @Email String email,
        @NotNull MembershipRole role
) {
}
