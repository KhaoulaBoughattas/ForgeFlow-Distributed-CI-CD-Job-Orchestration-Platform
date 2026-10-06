package dev.forgeflow.api.webhook;

import dev.forgeflow.api.common.web.BadRequestException;

public class InvalidWebhookPayloadException extends BadRequestException {
    public InvalidWebhookPayloadException(String message) {
        super(message);
    }
}
