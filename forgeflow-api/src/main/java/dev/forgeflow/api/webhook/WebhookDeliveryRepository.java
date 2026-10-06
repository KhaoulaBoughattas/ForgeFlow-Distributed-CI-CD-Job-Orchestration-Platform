package dev.forgeflow.api.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    boolean existsByGithubDeliveryId(String githubDeliveryId);
}
