package de.aivot.GoverBackend.core.models;

import jakarta.activation.MimeType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class HttpServiceHeaders {
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final Map<String, String> headers = new HashMap<>();

    public static HttpServiceHeaders create() {
        return new HttpServiceHeaders();
    }

    @Nonnull
    public HttpServiceHeaders with(@Nonnull String key, @Nonnull String value) {
        headers.put(key, value);
        return this;
    }

    @Nonnull
    public HttpServiceHeaders with(@Nullable HttpServiceHeaders headers) {
        if (headers != null) {
            this.headers.putAll(headers.headers);
        }
        return this;
    }

    public void forEach(@Nonnull BiConsumer<String, String> action) {
        headers.forEach(action);
    }
}
