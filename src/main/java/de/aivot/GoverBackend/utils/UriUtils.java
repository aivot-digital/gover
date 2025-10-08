package de.aivot.GoverBackend.utils;

import java.net.URI;

/**
 * Utility class for constructing URIs from a hostname and optional path segments.
 */
public class UriUtils {
    /**
     * Creates a URI from the given hostname and optional path segments.
     * <p>
     * If the hostname does not start with "http://" or "https://", "http://" is prepended.
     * Ensures that path segments are properly joined with slashes, avoiding duplicate or missing slashes.
     * </p>
     *
     * @param hostname the base hostname, with or without protocol (e.g., "example.com" or "https://example.com")
     * @param path     optional path segments to append to the hostname
     * @return a {@link URI} representing the constructed URI
     */
    public static URI create(String hostname, String... path) {
        var builder = new StringBuilder();

        if (!hostname.startsWith("http://") && !hostname.startsWith("https://")) {
            builder.append("http://");
        }

        builder.append(hostname);

        if (!builder.toString().endsWith("/")) {
            builder.append("/");
        }

        for (var p : path) {
            if (StringUtils.isNullOrEmpty(p)) {
                continue;
            }

            if (p.startsWith("/")) {
                p = p.substring(1);
            }

            if (!builder.toString().endsWith("/")) {
                builder.append("/");
            }

            builder.append(p);
        }

        return URI.create(builder.toString());
    }
}
