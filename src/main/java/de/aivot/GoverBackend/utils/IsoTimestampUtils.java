package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class IsoTimestampUtils {
    private IsoTimestampUtils() {
    }

    @Nonnull
    public static String nowUtc() {
        return Instant.now().toString();
    }

    @Nonnull
    public static String toUtcString(@Nonnull Instant value) {
        return value.toString();
    }

    @Nonnull
    public static String toUtcString(@Nonnull LocalDateTime value) {
        return value.atOffset(ZoneOffset.UTC).toInstant().toString();
    }

    @Nonnull
    public static Instant parseIsoTimestamp(@Nonnull String value, @Nonnull ZoneId fallbackZone) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // Legacy values were stored without an offset. We need an explicit fallback zone to
        // convert them into an absolute instant without silently depending on the JVM default.
        return LocalDateTime
                .parse(value, DateTimeFormatter.ISO_DATE_TIME)
                .atZone(fallbackZone)
                .toInstant();
    }
}
