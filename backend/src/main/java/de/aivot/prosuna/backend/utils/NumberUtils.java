package de.aivot.prosuna.backend.utils;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class NumberUtils {
    public static final int DEFAULT_SCALE = 0;
    // Security: numbers with more digits before or after the decimal point are rejected before rounding or
    // calculating with them, because text such as "1e300000000" would expand to hundreds of millions of digits.
    public static final int MAX_NUMBER_DIGITS = 100;

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value) {
        return formatGermanNumber(value, DEFAULT_SCALE);
    }

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value, int scale) {
        return formatGermanNumber(value, scale, scale);
    }

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value, int minScale, int maxScale) {
        Locale locale = Locale.GERMAN;
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(minScale);
        formatter.setMaximumFractionDigits(maxScale);
        return formatter.format(value);
    }

    @Nullable
    public static BigDecimal parseGermanNumber(@Nonnull String value) {
        return parseGermanNumber(value, DEFAULT_SCALE);
    }

    @Nullable
    public static BigDecimal parseGermanNumber(@Nonnull String value, int scale) {
        var normalizedValue = value
                .replaceAll("\\.", "")
                .replace(",", ".");

        try {
            return roundHalfUp(new BigDecimal(normalizedValue), scale);
        } catch (NumberFormatException exp) {
            try {
                var iVal = new BigInteger(normalizedValue);
                return BigDecimal
                        .valueOf(iVal.doubleValue())
                        .setScale(scale, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }

    /**
     * Whether the number has more than {@link #MAX_NUMBER_DIGITS} digits before or after the decimal point.
     */
    public static boolean exceedsDigitLimit(@Nonnull BigDecimal value) {
        return countIntegerDigits(value) > MAX_NUMBER_DIGITS || value.scale() > MAX_NUMBER_DIGITS;
    }

    /**
     * Rounds half up to the given decimal places. Returns null for numbers with more than {@link #MAX_NUMBER_DIGITS}
     * digits before the decimal point.
     */
    @Nullable
    public static BigDecimal roundHalfUp(@Nonnull BigDecimal value, int scale) {
        var integerDigits = countIntegerDigits(value);
        if (integerDigits > MAX_NUMBER_DIGITS) {
            return null;
        }
        // Below half a unit of the last decimal place, so the result is zero. Rounding text such as "1e-300000000"
        // would divide by an enormous power of ten.
        if (integerDigits < -(long) scale) {
            return BigDecimal.ZERO.setScale(scale);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    // Digits before the decimal point, zero or negative below 1. Precision minus scale overflows int for "1e2147483647".
    private static long countIntegerDigits(@Nonnull BigDecimal value) {
        return (long) value.precision() - value.scale();
    }

    @Nonnull
    public static Optional<Number> asNumber(Object value) {
        return switch (value) {
            case Number n -> Optional.of(n);
            case String s -> Optional.of(new BigDecimal(s));
            case Boolean b -> Optional.of(b ? 1 : 0);
            default -> Optional.empty();
        };
    }
}
