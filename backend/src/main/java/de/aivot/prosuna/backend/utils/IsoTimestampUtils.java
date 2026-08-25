package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;

public final class IsoTimestampUtils {
    // Shape validation keeps ISO variants without seconds and named zones out before
    // the JDK parser gets a chance to accept them.
    private static final Pattern EXPLICIT_INSTANT_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:\\d{2})$"
    );
    private static final Pattern LOCAL_DATE_TIME_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?$"
    );
    // The no-offset text is explicitly "+00:00" so UTC follows the same numeric-offset
    // wire convention as every other application-timezone offset instead of becoming "Z".
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendOffset("+HH:MM", "+00:00")
            .toFormatter();

    private IsoTimestampUtils() {
    }

    @Nonnull
    public static String nowWithOffset(@Nonnull ZoneId zoneId) {
        return toOffsetString(Instant.now(), zoneId);
    }

    @Nonnull
    public static String toOffsetString(@Nonnull Instant value) {
        return toOffsetString(value, ApplicationTimeZone.getZoneId());
    }

    @Nonnull
    public static String toOffsetString(@Nonnull Instant value, @Nonnull ZoneId zoneId) {
        return ISO_OFFSET_DATE_TIME.format(value.atZone(zoneId));
    }

    @Nonnull
    public static Instant parseIsoInstant(@Nonnull String value) {
        if (EXPLICIT_INSTANT_PATTERN.matcher(value).matches()) {
            return OffsetDateTime
                    .parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant();
        }

        if (!LOCAL_DATE_TIME_PATTERN.matcher(value).matches()) {
            throw new DateTimeParseException(
                    "Expected an ISO-8601 timestamp with seconds",
                    value,
                    0
            );
        }

        var localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var zoneId = ApplicationTimeZone.getZoneId();
        var validOffsets = zoneId.getRules().getValidOffsets(localDateTime);
        if (validOffsets.isEmpty()) {
            throw new DateTimeParseException(
                    "Local date-time does not exist in application timezone " + zoneId,
                    value,
                    0
            );
        }

        return localDateTime
                .atOffset(validOffsets.getFirst())
                .toInstant();
    }

}
