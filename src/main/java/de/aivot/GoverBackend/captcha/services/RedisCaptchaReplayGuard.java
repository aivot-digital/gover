package de.aivot.GoverBackend.captcha.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Prevents replay attacks by rejecting duplicate payloads.
 *
 * Altcha widget payloads are one-time proof-of-work tokens.
 * This guard stores a hash of each incoming payload in Redis and
 * blocks any re-use for 5 minutes.
 */
@Service
public class RedisCaptchaReplayGuard {

    private static final String REDIS_PREFIX = "captcha:payload:";
    private static final long TTL_MINUTES = 5;

    private final StringRedisTemplate redis;

    public RedisCaptchaReplayGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Returns true if this payload has already been used.
     * Otherwise, marks it as seen for 5 minutes.
     */
    public boolean isUsed(String base64Payload) {
        String hash = hashBase64(base64Payload);
        String key = REDIS_PREFIX + hash;

        // Try to SET key if not exists (NX) with TTL
        Boolean wasInserted = redis.opsForValue()
                .setIfAbsent(key, "1", TTL_MINUTES, TimeUnit.MINUTES);

        return wasInserted == null || !wasInserted;
    }

    private String hashBase64(String base64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] decoded = Base64.getDecoder().decode(base64);
            byte[] hashed = digest.digest(decoded);
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash captcha payload", e);
        }
    }
}