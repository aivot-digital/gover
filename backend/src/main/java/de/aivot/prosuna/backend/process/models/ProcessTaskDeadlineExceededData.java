package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.Map;

/** System-owned element data; the deadline itself identifies the reason for expiry. */
public record ProcessTaskDeadlineExceededData(
        @Nonnull String deadlineKey,
        @Nonnull String label,
        @Nonnull Instant deadline,
        @Nonnull Instant processedAt
) {
    public static final String NODE_DATA_KEY = "_deadlineExceeded";

    @Nonnull
    public Map<String, Object> toMap() {
        return Map.of("deadlineKey", deadlineKey, "label", label,
                "deadline", deadline.toString(), "processedAt", processedAt.toString());
    }
}
