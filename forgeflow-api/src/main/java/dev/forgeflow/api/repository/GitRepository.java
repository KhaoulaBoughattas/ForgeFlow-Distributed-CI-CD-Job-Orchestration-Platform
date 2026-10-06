package dev.forgeflow.api.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A GitHub repository connected to a project. The webhook secret is persisted as
 * webhookSecretCiphertext, AES-256-GCM encrypted at rest via
 * {@link dev.forgeflow.api.common.security.WebhookSecretCipher} -- reversible, unlike a
 * password hash, because verifying each webhook's HMAC-SHA256 signature requires the plaintext
 * secret. The plaintext itself is shown to the user exactly once, at connect time.
 */
@Entity
@Table(name = "git_repositories")
public class GitRepository implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "github_owner", nullable = false)
    private String githubOwner;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(name = "webhook_secret_ciphertext", nullable = false)
    private String webhookSecretCiphertext;

    @Column(nullable = false)
    private boolean connected;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected GitRepository() {
        // JPA
    }

    private GitRepository(UUID id, UUID projectId, String githubOwner, String githubRepo,
                           String webhookSecretCiphertext, boolean connected, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.webhookSecretCiphertext = webhookSecretCiphertext;
        this.connected = connected;
        this.createdAt = createdAt;
    }

    public static GitRepository connect(UUID projectId, String githubOwner, String githubRepo, String webhookSecretCiphertext) {
        return new GitRepository(UUID.randomUUID(), projectId, githubOwner, githubRepo, webhookSecretCiphertext, true, Instant.now());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public String getWebhookSecretCiphertext() {
        return webhookSecretCiphertext;
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        this.connected = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
