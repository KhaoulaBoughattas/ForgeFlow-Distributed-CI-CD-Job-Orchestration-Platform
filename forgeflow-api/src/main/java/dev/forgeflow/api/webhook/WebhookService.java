package dev.forgeflow.api.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.forgeflow.api.common.security.WebhookSecretCipher;
import dev.forgeflow.api.pipeline.PipelineResponse;
import dev.forgeflow.api.pipeline.PipelineService;
import dev.forgeflow.api.repository.GitRepository;
import dev.forgeflow.api.repository.RepositoryService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Validates and processes incoming GitHub "push" webhooks: verifies the HMAC signature against
 * the target repository's decrypted secret, enforces delivery idempotency, and admits a new
 * pipeline run for the pushed commit.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final Pattern SHA_PATTERN = Pattern.compile("^[a-f0-9]{7,40}$");

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookSecretCipher webhookSecretCipher;
    private final RepositoryService repositoryService;
    private final PipelineService pipelineService;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public WebhookService(
            WebhookSignatureVerifier signatureVerifier,
            WebhookSecretCipher webhookSecretCipher,
            RepositoryService repositoryService,
            PipelineService pipelineService,
            WebhookDeliveryRepository webhookDeliveryRepository,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.webhookSecretCipher = webhookSecretCipher;
        this.repositoryService = repositoryService;
        this.pipelineService = pipelineService;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookResponse handlePush(String rawPayload, String signatureHeader, String deliveryId, String eventType) {
        meterRegistry.counter("forgeflow.webhooks.received").increment();

        if (deliveryId == null || deliveryId.isBlank()) {
            throw new InvalidWebhookPayloadException("Missing X-GitHub-Delivery header");
        }

        if (!"push".equals(eventType)) {
            // We only act on pushes; ping/other event types are acknowledged but ignored.
            return WebhookResponse.ignored();
        }

        GitHubPushPayload payload = parsePayload(rawPayload);
        if (payload.repository() == null || payload.repository().owner() == null) {
            throw new InvalidWebhookPayloadException("Payload is missing repository information");
        }

        String owner = payload.repository().owner().login();
        String repoName = payload.repository().name();
        GitRepository repository = repositoryService.requireConnectedRepository(owner, repoName);

        String plaintextSecret = webhookSecretCipher.decrypt(repository.getWebhookSecretCiphertext());
        if (!signatureVerifier.verify(rawPayload, signatureHeader, plaintextSecret)) {
            throw new WebhookSignatureInvalidException();
        }

        String commitSha = payload.afterCommitSha();
        String branch = payload.branchName();
        if (commitSha == null || !SHA_PATTERN.matcher(commitSha).matches()) {
            throw new InvalidWebhookPayloadException("Payload has a missing or malformed commit SHA");
        }
        if (branch == null || branch.isBlank()) {
            throw new InvalidWebhookPayloadException("Payload is missing a branch ref");
        }

        if (!recordDeliveryIfNew(deliveryId)) {
            log.info("Ignoring duplicate webhook delivery {}", deliveryId);
            return WebhookResponse.duplicate();
        }

        PipelineResponse pipeline = pipelineService.create(repository, commitSha, branch);
        log.info("Admitted pipeline {} for {}/{}@{}", pipeline.id(), owner, repoName, commitSha);
        return WebhookResponse.accepted(pipeline.id());
    }

    /** Returns true if this delivery had not been seen before (and should be processed). */
    private boolean recordDeliveryIfNew(String deliveryId) {
        if (webhookDeliveryRepository.existsByGithubDeliveryId(deliveryId)) {
            return false;
        }
        try {
            webhookDeliveryRepository.saveAndFlush(WebhookDelivery.recordNew(deliveryId));
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Lost a race with a concurrent delivery of the same id hitting the unique
            // constraint at the DB level -- treat it the same as "already seen".
            return false;
        }
    }

    private GitHubPushPayload parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, GitHubPushPayload.class);
        } catch (JsonProcessingException ex) {
            throw new InvalidWebhookPayloadException("Could not parse webhook payload as JSON");
        }
    }
}
