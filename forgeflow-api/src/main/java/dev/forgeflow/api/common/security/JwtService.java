package dev.forgeflow.api.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256 JWTs for session authentication. The signing secret and token
 * lifetime are externalized via forgeflow.security.jwt.*.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${forgeflow.security.jwt.secret}") String secret,
            @Value("${forgeflow.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and verifies the token's signature and expiry, returning the authenticated
     * principal. Throws IllegalArgumentException (wrapping the underlying JwtException) on
     * any invalid, tampered or expired token so callers have a single failure type to handle.
     */
    public AuthenticatedUser parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("email", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid or expired token", ex);
        }
    }
}
