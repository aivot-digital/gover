package de.aivot.prosuna.backend.ai.services;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public final class AiToolResults {
    private AiToolResults() {}

    public record Page<T>(@Nonnull List<T> items, @Nullable Integer nextOffset, int total) {}
    public record Value(@Nonnull String json, boolean truncated, @Nullable Integer nextOffset, int length) {}

    @Nonnull
    public static <T> Page<T> page(@Nonnull List<T> values, @Nullable Integer offset, @Nullable Integer limit) {
        int start = offset == null ? 0 : offset;
        int size = limit == null ? 20 : limit;
        if (start < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Offset muss mindestens 0 und Limit zwischen 1 und 50 sein.");
        start = Math.min(start, values.size());
        int end = Math.min(values.size(), start + size);
        return new Page<>(values.subList(start, end), end < values.size() ? end : null, values.size());
    }

    @Nonnull
    public static Value value(@Nonnull JsonMapper mapper, @Nullable Object value, int offset, int length) {
        var json = mapper.writeValueAsString(value);
        if (offset < 0 || offset > json.length()) throw new IllegalArgumentException("Der Wert-Offset ist ungültig.");
        var end = Math.min(json.length(), offset + length);
        return new Value(json.substring(offset, end), offset > 0 || end < json.length(), end < json.length() ? end : null, json.length());
    }

    public static boolean matches(@Nullable String query, @Nullable Object value) {
        return query == null || query.isBlank() || String.valueOf(value).toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT));
    }
}
