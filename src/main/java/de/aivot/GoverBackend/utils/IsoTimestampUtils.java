package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class IsoTimestampUtils {
    private IsoTimestampUtils() {
    }

    @Nonnull
    public static String nowUtc() {
        return Instant.now().toString();
    }

    @Nonnull
    public static String toUtcString(@Nonnull LocalDateTime value) {
        return value.atOffset(ZoneOffset.UTC).toInstant().toString();
    }
}
