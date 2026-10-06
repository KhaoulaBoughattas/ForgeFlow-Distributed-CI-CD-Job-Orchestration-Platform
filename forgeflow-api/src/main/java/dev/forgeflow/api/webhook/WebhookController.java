package dev.forgeflow.api.webhook;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Reads the raw request body ourselves (rather than binding to a DTO) because the HMAC
     * signature must be computed over the exact bytes GitHub sent -- re-serializing a
     * deserialized object would not reliably reproduce them.
     */
    @PostMapping("/github")
    public WebhookResponse github(
            HttpServletRequest request,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) throws IOException {

        String rawPayload = readBody(request);
        return webhookService.handlePush(rawPayload, signature, deliveryId, eventType);
    }

    private String readBody(HttpServletRequest request) throws IOException {
        try (InputStream inputStream = request.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
