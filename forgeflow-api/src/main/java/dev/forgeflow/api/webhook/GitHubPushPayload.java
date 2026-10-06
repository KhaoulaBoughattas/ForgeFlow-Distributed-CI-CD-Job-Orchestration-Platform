package dev.forgeflow.api.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Minimal shape of a GitHub "push" webhook event -- only the fields ForgeFlow actually needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPushPayload(
        String ref,
        @JsonProperty("after") String afterCommitSha,
        GitHubRepositoryPayload repository
) {
    /** "refs/heads/main" -> "main". */
    public String branchName() {
        if (ref == null) {
            return null;
        }
        String prefix = "refs/heads/";
        return ref.startsWith(prefix) ? ref.substring(prefix.length()) : ref;
    }
}
