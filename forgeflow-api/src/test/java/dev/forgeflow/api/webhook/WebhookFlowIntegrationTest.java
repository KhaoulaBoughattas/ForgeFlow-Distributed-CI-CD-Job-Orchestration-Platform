package dev.forgeflow.api.webhook;

import dev.forgeflow.api.AbstractIntegrationTest;
import dev.forgeflow.api.auth.AuthResponse;
import dev.forgeflow.api.auth.RegisterRequest;
import dev.forgeflow.api.organization.CreateOrganizationRequest;
import dev.forgeflow.api.organization.OrganizationResponse;
import dev.forgeflow.api.project.CreateProjectRequest;
import dev.forgeflow.api.project.ProjectResponse;
import dev.forgeflow.api.repository.ConnectRepositoryRequest;
import dev.forgeflow.api.repository.ConnectRepositoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class WebhookFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void aValidSignedPushIsAcceptedAndAdmitsAPipeline() throws Exception {
        String token = registerAndGetToken("webhook-flow@example.com", "Webhook Flow");
        UUID projectId = createOrgAndProject(token, "webhook-org", "webhook-project");

        ConnectRepositoryResponse connected = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/repositories", HttpMethod.POST,
                authedEntity(token, new ConnectRepositoryRequest("octocat", "hello-world")),
                ConnectRepositoryResponse.class).getBody();

        String payload = "{"
                + "\"ref\":\"refs/heads/main\","
                + "\"after\":\"abc1234abc1234abc1234abc1234abc1234abc1\","
                + "\"repository\":{\"name\":\"hello-world\",\"owner\":{\"login\":\"octocat\"}}"
                + "}";
        String signature = "sha256=" + hmac(payload, connected.webhookSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hub-Signature-256", signature);
        headers.set("X-GitHub-Delivery", UUID.randomUUID().toString());
        headers.set("X-GitHub-Event", "push");

        ResponseEntity<WebhookResponse> response = restTemplate.exchange(
                "/api/v1/webhooks/github", HttpMethod.POST, new HttpEntity<>(payload, headers), WebhookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("accepted");
        assertThat(response.getBody().pipelineId()).isNotNull();
    }

    @Test
    void anIncorrectlySignedPushIsRejected() {
        String token = registerAndGetToken("webhook-bad-sig@example.com", "Webhook Bad Sig");
        UUID projectId = createOrgAndProject(token, "webhook-bad-org", "webhook-bad-project");

        restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/repositories", HttpMethod.POST,
                authedEntity(token, new ConnectRepositoryRequest("octocat", "bad-sig-repo")),
                ConnectRepositoryResponse.class);

        String payload = "{"
                + "\"ref\":\"refs/heads/main\","
                + "\"after\":\"abc1234abc1234abc1234abc1234abc1234abc1\","
                + "\"repository\":{\"name\":\"bad-sig-repo\",\"owner\":{\"login\":\"octocat\"}}"
                + "}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000");
        headers.set("X-GitHub-Delivery", UUID.randomUUID().toString());
        headers.set("X-GitHub-Event", "push");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/webhooks/github", HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String registerAndGetToken(String email, String displayName) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest(email, "correct-horse-battery", displayName), AuthResponse.class);
        return response.getBody().token();
    }

    private UUID createOrgAndProject(String token, String orgSlug, String projectSlug) {
        var organizationId = restTemplate.exchange(
                "/api/v1/organizations", HttpMethod.POST,
                authedEntity(token, new CreateOrganizationRequest(orgSlug, orgSlug)),
                OrganizationResponse.class).getBody().id();

        return restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/projects", HttpMethod.POST,
                authedEntity(token, new CreateProjectRequest(projectSlug, projectSlug)),
                ProjectResponse.class).getBody().id();
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
