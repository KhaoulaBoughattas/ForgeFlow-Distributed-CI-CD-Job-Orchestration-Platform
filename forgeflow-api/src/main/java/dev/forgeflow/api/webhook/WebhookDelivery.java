package dev.forgeflow.api.webhook;

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
 * Records every GitHub delivery id we've successfully processed. GitHub retries webhook
 * deliveries on timeout/5xx, so the same event can arrive more than once; the unique constraint
 * on githubDeliveryId (see V7 migration) is what actually enforces idempotency -- the
 * save-then-catch-DataIntegrityViolationException dance in WebhookService exists only to turn
 * that DB-level rejection into a clean "duplicate" response instead of a 500.
 */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "github_delivery_id", nullable = false, unique = true)
    private String githubDeliveryId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Transient
    private boolean isNew = true;

    protected WebhookDelivery() {
        // JPA
    }

    private WebhookDelivery(UUID id, String githubDeliveryId, Instant receivedAt) {
        this.id = id;
        this.githubDeliveryId = githubDeliveryId;
        this.receivedAt = receivedAt;
    }

    public static WebhookDelivery recordNew(String githubDeliveryId) {
        return new WebhookDelivery(UUID.randomUUID(), githubDeliveryId, Instant.now());
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

    public String getGithubDeliveryId() {
        return githubDeliveryId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
