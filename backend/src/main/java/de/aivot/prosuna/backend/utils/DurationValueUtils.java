package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public final class DurationValueUtils {
    // Keep seconds exactly representable across JSON, JavaScript and Java.
    public static final long MAX_SECONDS = 9_007_199_254_740_991L;
    public static final String INVALID_DURATION = "Bitte geben Sie eine Zeitdauer in ganzen, nicht negativen Sekunden innerhalb des unterstützten Zahlenbereichs ein.";

    private DurationValueUtils() {}

    public static long requireSeconds(@Nonnull BigDecimal value) {
        try {
            var seconds = value.longValueExact();
            if (seconds < 0 || seconds > MAX_SECONDS) throw new ArithmeticException();
            return seconds;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(INVALID_DURATION);
        }
    }

    @Nonnull
    public static String formatSeconds(long seconds, boolean minutePrecision) {
        return formatSeconds(seconds, minutePrecision, DurationLargestUnit.Hour);
    }

    @Nonnull
    public static String formatSeconds(long seconds, boolean minutePrecision, @Nullable DurationLargestUnit largestUnit) {
        if (seconds < 0 || seconds > MAX_SECONDS) throw new IllegalArgumentException(INVALID_DURATION);
        var parts = new ArrayList<String>();
        // These are fixed 24-hour units, independent of calendar dates and daylight-saving transitions.
        var days = largestUnit == DurationLargestUnit.Day ? seconds / 86400 : 0;
        var hours = (largestUnit == DurationLargestUnit.Day ? seconds % 86400 : seconds) / 3600;
        var minutes = seconds % 3600 / 60;
        var remainder = seconds % 60;
        if (days > 0) parts.add(NumberFormat.getIntegerInstance(Locale.GERMANY).format(days) + (days == 1 ? " Tag" : " Tage"));
        if (hours > 0) parts.add(NumberFormat.getIntegerInstance(Locale.GERMANY).format(hours) + (hours == 1 ? " Stunde" : " Stunden"));
        if (minutes > 0) parts.add(minutes + (minutes == 1 ? " Minute" : " Minuten"));
        if (remainder > 0) parts.add(remainder + (remainder == 1 ? " Sekunde" : " Sekunden"));
        return parts.isEmpty() ? (minutePrecision ? "0 Minuten" : "0 Sekunden") : String.join(" ", parts);
    }
}
