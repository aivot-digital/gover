package de.aivot.GoverBackend.captcha.services;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Prevents replay attacks by rejecting duplicate payloads.
 *
 * Altcha widget payloads are one-time proof-of-work tokens.
 * This guard stores a hash of each incoming payload in memory and
 * blocks any re-use for 5 minutes.
 */
@Service
public class CaptchaReplayGuard {

    private final Set<String> payloadHashes = ConcurrentHashMap.newKeySet();

    public boolean isUsed(String payload) {
        String hash = hashBase64(payload);
        if (payloadHashes.contains(hash)) {
            return true;
        }
        payloadHashes.add(hash);
        // Schedule removal of the hash after 5 minutes
        Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> payloadHashes.remove(hash), 5, TimeUnit.MINUTES);
        return false;
    }

    private String hashBase64(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] decoded = Base64.getDecoder().decode(input);
            byte[] hash = digest.digest(decoded);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash payload", e);
        }
    }
}
