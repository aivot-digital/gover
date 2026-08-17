package de.aivot.gover.backend.utils;

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
        var bytes = generateRandomBytes(length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
