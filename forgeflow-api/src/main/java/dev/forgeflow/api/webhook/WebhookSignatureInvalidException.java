package dev.forgeflow.api.webhook;

import dev.forgeflow.api.common.web.UnauthorizedException;

public class WebhookSignatureInvalidException extends UnauthorizedException {
    public WebhookSignatureInvalidException() {
        super("Webhook signature is missing or invalid");
    }
}
