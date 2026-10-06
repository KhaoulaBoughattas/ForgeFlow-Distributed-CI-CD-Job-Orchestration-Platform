package dev.forgeflow.api.repository;

/**
 * Returned exactly once, right after connecting a repository: the only time the plaintext
 * webhook secret is ever available. The caller must configure it as the GitHub webhook's
 * secret immediately, since only a BCrypt hash of it is retained server-side afterward.
 */
public record ConnectRepositoryResponse(RepositoryResponse repository, String webhookSecret) {
}
