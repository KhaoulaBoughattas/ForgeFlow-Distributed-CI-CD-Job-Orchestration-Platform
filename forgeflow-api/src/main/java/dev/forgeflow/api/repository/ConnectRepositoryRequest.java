package dev.forgeflow.api.repository;

import jakarta.validation.constraints.NotBlank;

public record ConnectRepositoryRequest(
        @NotBlank String githubOwner,
        @NotBlank String githubRepo
) {
}
