package dev.forgeflow.api.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryPayload(String name, GitHubOwnerPayload owner) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubOwnerPayload(String login) {
    }
}
