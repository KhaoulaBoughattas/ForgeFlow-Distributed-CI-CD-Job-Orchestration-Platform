package dev.forgeflow.api.auth;

import dev.forgeflow.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerThenLoginReturnsAWorkingToken() {
        RegisterRequest registerRequest = new RegisterRequest("flow@example.com", "correct-horse-battery", "Flow User");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().token()).isNotBlank();
        assertThat(registerResponse.getBody().user().email()).isEqualTo("flow@example.com");

        LoginRequest loginRequest = new LoginRequest("flow@example.com", "correct-horse-battery");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().token()).isNotBlank();
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() {
        RegisterRequest registerRequest = new RegisterRequest("dup@example.com", "correct-horse-battery", "Dup User");
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        ResponseEntity<String> second = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        RegisterRequest registerRequest = new RegisterRequest("wrongpass@example.com", "correct-horse-battery", "Wrong Pass");
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        LoginRequest loginRequest = new LoginRequest("wrongpass@example.com", "incorrect-password");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
