package dev.forgeflow.api.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies GitHub's `X-Hub-Signature-256: sha256=<hex>` header: an HMAC-SHA256 of the raw
 * request body keyed by the repository's webhook secret. Uses MessageDigest.isEqual for the
 * final comparison, which runs in constant time regardless of where the strings first differ,
 * so this cannot be used as a timing oracle to guess the correct signature byte by byte.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    public boolean verify(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        String providedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] provided = HexFormat.of().parseHex(providedHex);
            return MessageDigest.isEqual(computed, provided);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException ex) {
            return false;
        }
    }
}
