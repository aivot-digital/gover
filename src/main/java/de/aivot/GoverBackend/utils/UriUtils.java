package de.aivot.GoverBackend.utils;

import java.net.URI;

/**
 * Utility class for constructing URIs from a hostname and optional path segments.
 * <p>
 * This class provides static methods to build URIs and URI strings, ensuring proper protocol handling and path joining.
 * </p>
 */
public class UriUtils {
    /**
     * Creates a URI from the given hostname and optional path segments.
     * <p>
     * If the hostname does not start with "http://" or "https://", "http://" is prepended.
     * Path segments are joined to the hostname, ensuring single slashes between segments and omitting empty segments.
     * </p>
     *
     * @param hostname the base hostname, with or without protocol (e.g., "example.com" or "https://example.com")
     * @param path     optional path segments to append to the hostname; empty or null segments are ignored
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

    /**
     * Creates a URI string from the given hostname and optional path segments.
     * <p>
     * This is a convenience method that calls {@link #create(String, String...)} and returns the URI as a string.
     * </p>
     *
     * @param hostname the base hostname, with or without protocol
     * @param path     optional path segments to append to the hostname
     * @return a string representation of the constructed URI
     */
    public static String createString(String hostname, String ... path) {
        return create(hostname, path).toString();
    }
}
