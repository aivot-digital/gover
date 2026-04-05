package de.aivot.GoverBackend.utils;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public class NumberUtils {
    public static final int DEFAULT_SCALE = 0;

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value) {
        return formatGermanNumber(value, DEFAULT_SCALE);
    }

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value, int scale) {
        Locale locale = Locale.GERMAN;
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(scale);
        formatter.setMaximumFractionDigits(scale);
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
            return new BigDecimal(normalizedValue)
                    .setScale(scale, RoundingMode.HALF_UP);
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
}
