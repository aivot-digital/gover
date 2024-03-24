package de.aivot.GoverBackend.utils;


import jakarta.annotation.Nullable;

import java.util.Random;

public class StringUtils {
    public static String generateRandomString(int length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static boolean isNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(@Nullable String str) {
        return !isNullOrEmpty(str);
    }
}
