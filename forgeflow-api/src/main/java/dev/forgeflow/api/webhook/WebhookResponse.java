package dev.forgeflow.api.webhook;

import java.util.UUID;

public record WebhookResponse(String status, UUID pipelineId) {
    public static WebhookResponse accepted(UUID pipelineId) {
        return new WebhookResponse("accepted", pipelineId);
    }

    public static WebhookResponse ignored() {
        return new WebhookResponse("ignored", null);
    }

    public static WebhookResponse duplicate() {
        return new WebhookResponse("duplicate", null);
    }
}
