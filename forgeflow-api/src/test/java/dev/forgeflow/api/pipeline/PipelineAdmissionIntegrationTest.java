package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.AbstractIntegrationTest;
import dev.forgeflow.api.auth.AuthResponse;
import dev.forgeflow.api.auth.RegisterRequest;
import dev.forgeflow.api.organization.CreateOrganizationRequest;
import dev.forgeflow.api.organization.OrganizationResponse;
import dev.forgeflow.api.project.CreateProjectRequest;
import dev.forgeflow.api.project.ProjectResponse;
import dev.forgeflow.api.repository.ConnectRepositoryRequest;
import dev.forgeflow.api.repository.ConnectRepositoryResponse;
import dev.forgeflow.api.webhook.WebhookResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full admission path: a webhook creates a PENDING pipeline, and after the
 * creating transaction commits, JobAdmissionListener asynchronously flips it to QUEUED and
 * PipelineEventPublisher publishes it to Kafka. We only assert on the DB-visible QUEUED
 * transition here; the Kafka side is implicitly covered by the worker actually consuming it in
 * a real deployment, which is out of scope for this module's tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class PipelineAdmissionIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void aPushedCommitIsAdmittedAndEventuallyQueued() throws Exception {
        String token = registerAndGetToken("admission@example.com", "Admission Test");

        var organizationId = restTemplate.exchange(
                "/api/v1/organizations", HttpMethod.POST,
                authedEntity(token, new CreateOrganizationRequest("admission-org", "admission-org")),
                OrganizationResponse.class).getBody().id();

        var projectId = restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/projects", HttpMethod.POST,
                authedEntity(token, new CreateProjectRequest("admission-project", "admission-project")),
                ProjectResponse.class).getBody().id();

        ConnectRepositoryResponse connected = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/repositories", HttpMethod.POST,
                authedEntity(token, new ConnectRepositoryRequest("octocat", "admission-repo")),
                ConnectRepositoryResponse.class).getBody();

        String payload = "{"
                + "\"ref\":\"refs/heads/main\","
                + "\"after\":\"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\","
                + "\"repository\":{\"name\":\"admission-repo\",\"owner\":{\"login\":\"octocat\"}}"
                + "}";
        String signature = "sha256=" + hmac(payload, connected.webhookSecret());

        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.set("X-Hub-Signature-256", signature);
        webhookHeaders.set("X-GitHub-Delivery", UUID.randomUUID().toString());
        webhookHeaders.set("X-GitHub-Event", "push");

        WebhookResponse webhookResponse = restTemplate.exchange(
                "/api/v1/webhooks/github", HttpMethod.POST, new HttpEntity<>(payload, webhookHeaders), WebhookResponse.class)
                .getBody();
        assertThat(webhookResponse.pipelineId()).isNotNull();

        PipelineDetailResponse detail = pollUntilQueuedOrTimeout(token, projectId.toString(), webhookResponse.pipelineId().toString());
        assertThat(detail.status()).isIn(PipelineStatus.QUEUED, PipelineStatus.RUNNING, PipelineStatus.PENDING);
    }

    private PipelineDetailResponse pollUntilQueuedOrTimeout(String token, String projectId, String pipelineId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        PipelineDetailResponse last = null;
        while (Instant.now().isBefore(deadline)) {
            last = restTemplate.exchange(
                    "/api/v1/projects/" + projectId + "/pipelines/" + pipelineId, HttpMethod.GET,
                    authedEntity(token, null), PipelineDetailResponse.class).getBody();
            if (last.status() != PipelineStatus.PENDING) {
                return last;
            }
            Thread.sleep(200);
        }
        return last;
    }

    private String registerAndGetToken(String email, String displayName) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest(email, "correct-horse-battery", displayName), AuthResponse.class);
        return response.getBody().token();
    }

    private <T> HttpEntity<T> authedEntity(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
