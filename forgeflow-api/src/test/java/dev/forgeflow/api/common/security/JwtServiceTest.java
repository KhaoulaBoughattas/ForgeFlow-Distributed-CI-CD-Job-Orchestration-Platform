package dev.forgeflow.api.common.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "unit-test-secret-value-at-least-32-characters-long", 60);

    @Test
    void issuesATokenThatParsesBackToTheSamePrincipal() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "someone@example.com");

        AuthenticatedUser parsed = jwtService.parseAndValidate(token);

        assertThat(parsed.id()).isEqualTo(userId);
        assertThat(parsed.email()).isEqualTo("someone@example.com");
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateToken(UUID.randomUUID(), "someone@example.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptyToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
