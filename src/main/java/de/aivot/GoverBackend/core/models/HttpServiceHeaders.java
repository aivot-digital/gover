package de.aivot.GoverBackend.core.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Utility class for building and managing HTTP headers for service requests.
 * <p>
 * Provides a fluent API for constructing HTTP headers, including common content types and authorization schemes.
 * Supports combining multiple header sets and iterating over header entries.
 * </p>
 * <h2>Usage Example:</h2>
 * <pre>
 *     HttpServiceHeaders headers = HttpServiceHeaders.create()
 *         .withContentType(HttpServiceHeaders.APPLICATION_JSON)
 *         .withAccept(HttpServiceHeaders.APPLICATION_JSON)
 *         .withAuthorizationBearer("token");
 * </pre>
 */
public class HttpServiceHeaders {
    /**
     * Constant for the "application/json" content type.
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * Constant for the "application/xml" content type.
     */
    public static final String APPLICATION_XML = "application/xml";

    /**
     * Constant for the "text/plain" content type.
     */
    public static final String TEXT_PLAIN = "text/plain";

    /**
     * Constant for the "text/html" content type.
     */
    public static final String TEXT_HTML = "text/html";

    /**
     * Constant for the "multipart/form-data" content type.
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * Constant for the "application/x-www-form-urlencoded" content type.
     */
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    /**
     * Internal map storing header key-value pairs.
     */
    private final Map<String, String> headers = new HashMap<>();

    /**
     * Creates a new, empty {@code HttpServiceHeaders} instance.
     *
     * @return a new {@code HttpServiceHeaders} object
     */
    public static HttpServiceHeaders create() {
        return new HttpServiceHeaders();
    }

    /**
     * Creates a new {@code HttpServiceHeaders} instance initialized with the provided headers.
     *
     * @param headers a map of header key-value pairs to initialize with; may be null
     * @return a new {@code HttpServiceHeaders} object containing the provided headers
     */
    public static HttpServiceHeaders of(Map<String, String> headers) {
        var httpHeaders = new HttpServiceHeaders();
        if (headers != null) {
            httpHeaders.headers.putAll(headers);
        }
        return httpHeaders;
    }

    /**
     * Adds or replaces a header entry with the specified key and value.
     *
     * @param value the header value (must not be null)
     * @return this {@code HttpServiceHeaders} instance for method chaining
     */
    @Nonnull
    public HttpServiceHeaders with(@Nonnull String key, @Nonnull String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Adds or replaces the "Content-Type" header with the specified value.
     *
     * @param value the content type value (must not be null)
     * @return this {@code HttpServiceHeaders} instance for method chaining
     */
    @Nonnull
    public HttpServiceHeaders withContentType(@Nonnull String value) {
        return this.with("Content-Type", value);
    }

    /**
     * Adds or replaces the "Accept" header with the specified value.
     *
     * @param value the accept header value (must not be null)
     * @return this {@code HttpServiceHeaders} instance for method chaining
     */
    @Nonnull
    public HttpServiceHeaders withAccept(@Nonnull String value) {
        return this.with("Accept", value);
    }

    /**
     * Adds or replaces the "Authorization" header with a Bearer token.
     *
     * @param value the bearer token (must not be null)
     * @return this {@code HttpServiceHeaders} instance for method chaining
     */
    @Nonnull
    public HttpServiceHeaders withAuthorizationBearer(@Nonnull String value) {
        return this.with("Authorization", "Bearer " + value);
    }

    /**
     * Adds all headers from another {@code HttpServiceHeaders} instance.
     *
     * @param headers another {@code HttpServiceHeaders} instance; may be null
     * @return this {@code HttpServiceHeaders} instance for method chaining
     */
    @Nonnull
    public HttpServiceHeaders with(@Nullable HttpServiceHeaders headers) {
        if (headers != null) {
            this.headers.putAll(headers.headers);
        }
        return this;
    }

    /**
     * Performs the given action for each header entry in this {@code HttpServiceHeaders} instance.
     *
     * @param action a {@code BiConsumer} that accepts the header key and value; must not be null
     */
    public void forEach(@Nonnull BiConsumer<String, String> action) {
        headers.forEach(action);
    }
}
