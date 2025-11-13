package de.aivot.GoverBackend.utils;


import jakarta.annotation.Nullable;

import java.util.Random;

public class StringUtils {
    public static String truncate(String string, int length) {
        if (string.length() > length) {
            return string.trim().substring(0, length).trim();
        }
        return string.trim();
    }

    public static String clean(String string, String pattern) {
        return string.replaceAll(pattern, "").replaceAll("\\R", " ");
    }

    public static String cleanAndTruncate(String string, String pattern, int length) {
        return truncate(clean(string, pattern), length);
    }

    public static boolean isNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(@Nullable String str) {
        return !isNullOrEmpty(str);
    }

    /**
     * Auto appends a slash to the end of the url if it is missing
     * @param url the url to normalize
     * @return the normalized url
     */
    public static String normalizeUrl(String url) {
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        if (url.endsWith("/")) {
            return url;
        }
        return url + "/";
    }

    /**
     * Obfuscates the key by replacing the middle characters with asterisks
     * @param key the key to obfuscate
     * @return the obfuscated key
     */
    public static String obfuscateKey(String key) {
        if (key == null) {
            return null;
        }
        if (key.length() < 4) {
            return "****";
        }
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }

    public static String slugify(String str, int length) {
        String slug = str.toLowerCase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");
        slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFKD);
        slug = slug.replaceAll("\\s+", "-")
                .replaceAll("[^\\w-]+", "")
                .replaceAll("-{2,}", "-");
        if (slug.length() > length) {
            slug = slug.substring(0, length);
        }
        return slug;
    }

    private static final String RANDOM_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomString(int length) {
        var result = new StringBuilder();
        var random = new Random();
        for (int i = 0; i < length; i++) {
            result.append(RANDOM_CHARACTERS.charAt(random.nextInt(RANDOM_CHARACTERS.length())));
        }
        return result.toString();
    }
}
