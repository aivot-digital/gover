package de.aivot.GoverBackend.utils;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;
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

    public static String quote(@Nullable String str) {
        if (str == null) {
            return "„“";
        }
        return "„" + str + "“";
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

    public static String capitalize(String key) {
        if (isNullOrEmpty(key)) {
            return key;
        }
        return key.substring(0, 1).toUpperCase() + key.substring(1);
    }

    public static String getSetterMethodName(String name) {
        return "set" + capitalize(name);
    }

    public static String encodeBase64String(String base64String) {
        return java.util.Base64.getEncoder().encodeToString(base64String.getBytes());
    }

    public static String decodeBase64String(String base64String) {
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64String);
        return new String(decodedBytes);
    }

    @Nonnull
    public static String getLastPathSegment(@Nullable String path) {
        if (isNullOrEmpty(path)) {
            return "";
        }
        String normalizedPath = path.replaceAll("/+$", "");
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return normalizedPath;
        }
        return normalizedPath.substring(lastSlashIndex + 1);
    }

    /**
     * Extracts the file extension from the given file name.
     * If the file name is null, empty, or does not contain a valid extension, an empty Optional is returned.
     *
     * @param fileName the file name to extract the extension from
     * @return an Optional containing the file extension in lowercase, or an empty Optional if no valid extension is found
     */
    public static Optional<String> extractExtensionFromFileName(@Nullable String fileName) {
        if (isNullOrEmpty(fileName)) {
            return Optional.empty();
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(lastDotIndex + 1).toLowerCase());
    }

    public static List<String> getPathSegments(String pathFromRoot) {
        if (isNullOrEmpty(pathFromRoot)) {
            return List.of();
        }
        String normalizedPath = pathFromRoot.replaceAll("/+", "/");
        String[] segments = normalizedPath.split("/");
        return List.of(segments);
    }

    public static String toNullableTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        var str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }
}
