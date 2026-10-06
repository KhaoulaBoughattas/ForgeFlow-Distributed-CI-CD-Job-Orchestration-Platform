package dev.forgeflow.api.organization;

import dev.forgeflow.api.AbstractIntegrationTest;
import dev.forgeflow.api.auth.AuthResponse;
import dev.forgeflow.api.auth.RegisterRequest;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class OrganizationRbacIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void creatorBecomesOwnerAndCanManageMembers() {
        String ownerToken = registerAndGetToken("owner-rbac@example.com", "Owner");
        String memberEmail = "member-rbac@example.com";
        registerAndGetToken(memberEmail, "Member");

        ResponseEntity<OrganizationResponse> createResponse = restTemplate.exchange(
                "/api/v1/organizations", HttpMethod.POST,
                authedEntity(ownerToken, new CreateOrganizationRequest("RBAC Test Org", "rbac-test-org")),
                OrganizationResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().yourRole()).isEqualTo(MembershipRole.OWNER);
        var organizationId = createResponse.getBody().id();

        ResponseEntity<MembershipResponse> addResponse = restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/members", HttpMethod.POST,
                authedEntity(ownerToken, new AddMemberRequest(memberEmail, MembershipRole.MEMBER)),
                MembershipResponse.class);

        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(addResponse.getBody().role()).isEqualTo(MembershipRole.MEMBER);
    }

    @Test
    void plainMemberCannotAddOtherMembers() {
        String ownerToken = registerAndGetToken("owner-perm@example.com", "Owner");
        String memberToken = registerAndGetToken("member-perm@example.com", "Member");
        String outsiderEmail = "outsider-perm@example.com";
        registerAndGetToken(outsiderEmail, "Outsider");

        var organizationId = restTemplate.exchange(
                "/api/v1/organizations", HttpMethod.POST,
                authedEntity(ownerToken, new CreateOrganizationRequest("Perm Org", "perm-org")),
                OrganizationResponse.class).getBody().id();

        restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/members", HttpMethod.POST,
                authedEntity(ownerToken, new AddMemberRequest("member-perm@example.com", MembershipRole.MEMBER)),
                MembershipResponse.class);

        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/members", HttpMethod.POST,
                authedEntity(memberToken, new AddMemberRequest(outsiderEmail, MembershipRole.MEMBER)),
                String.class);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void theLastOwnerCannotBeRemoved() {
        String ownerToken = registerAndGetToken("sole-owner@example.com", "Sole Owner");

        var organizationId = restTemplate.exchange(
                "/api/v1/organizations", HttpMethod.POST,
                authedEntity(ownerToken, new CreateOrganizationRequest("Sole Owner Org", "sole-owner-org")),
                OrganizationResponse.class).getBody().id();

        ResponseEntity<OrganizationResponse> self = restTemplate.exchange(
                "/api/v1/organizations/" + organizationId, HttpMethod.GET,
                authedEntity(ownerToken, null), OrganizationResponse.class);

        // Fetch the owner's own user id via /users/me to attempt self-removal.
        ResponseEntity<Map> me = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, authedEntity(ownerToken, null), Map.class);
        String ownerId = (String) me.getBody().get("id");

        ResponseEntity<String> removeResponse = restTemplate.exchange(
                "/api/v1/organizations/" + organizationId + "/members/" + ownerId, HttpMethod.DELETE,
                authedEntity(ownerToken, null), String.class);

        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.OK);
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
}
