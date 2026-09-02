package de.aivot.prosuna.backend.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static byte[] generateRandomBytes(int length) {
        var bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String generateRandomString(int length) {
        var bytes = generateRandomBytes(length / 2);  // Divide by two, to compensate for the Base64 encoding which increases the size by 2
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
