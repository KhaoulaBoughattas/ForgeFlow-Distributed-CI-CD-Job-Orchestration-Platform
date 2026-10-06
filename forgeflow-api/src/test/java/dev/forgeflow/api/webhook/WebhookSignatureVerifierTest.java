package dev.forgeflow.api.webhook;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    @Test
    void acceptsACorrectlyComputedSignature() throws Exception {
        String secret = "my-secret";
        String payload = "{\"ref\":\"refs/heads/main\"}";
        String signature = "sha256=" + computeHmac(payload, secret);

        assertThat(verifier.verify(payload, signature, secret)).isTrue();
    }

    @Test
    void rejectsASignatureComputedWithTheWrongSecret() throws Exception {
        String payload = "{\"ref\":\"refs/heads/main\"}";
        String signature = "sha256=" + computeHmac(payload, "wrong-secret");

        assertThat(verifier.verify(payload, signature, "my-secret")).isFalse();
    }

    @Test
    void rejectsAMissingHeader() {
        assertThat(verifier.verify("payload", null, "secret")).isFalse();
    }

    @Test
    void rejectsAMalformedHeader() {
        assertThat(verifier.verify("payload", "not-a-valid-signature", "secret")).isFalse();
    }

    private String computeHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
