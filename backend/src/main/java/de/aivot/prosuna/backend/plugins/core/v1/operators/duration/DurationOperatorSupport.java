package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

final class DurationOperatorSupport {
    private DurationOperatorSupport() {}

    @Nonnull
    static NoCodeParameter numberParameter(@Nonnull String label, @Nonnull String description) {
        return new NoCodeParameter(NoCodeDataType.Number, label, description)
                .withNumberConstraints(new NoCodeNumberConstraints(false, null, null, true));
    }

    @Nonnull
    static BigDecimal amount(@Nullable Object value, @Nonnull String label) throws NoCodeException {
        if (value == null || "".equals(value)) return BigDecimal.ZERO;

        try {
            var number = switch (value) {
                case BigDecimal decimal -> decimal;
                // Match the float-to-double conversion used by the original eight-place rounding.
                case Float floatValue -> BigDecimal.valueOf(floatValue.doubleValue());
                case Number numericValue -> new BigDecimal(numericValue.toString());
                case String text -> new BigDecimal(text.trim());
                default -> null;
            };
            if (number != null) {
                // Scaling up a large exponent or dividing by an enormous power of ten is unnecessary.
                if (number.scale() <= 8) return number;
                if (number.abs().compareTo(new BigDecimal("0.000000005")) < 0) return BigDecimal.ZERO;
                return number.setScale(8, RoundingMode.HALF_UP);
            }
        } catch (NumberFormatException ignored) {
            // Invalid text and non-finite numbers receive the same field-specific error.
        }
        throw new NoCodeException("Geben Sie für „" + label + "“ eine Zahl ein.");
    }

    @Nonnull
    static NoCodeParameter unitParameter() {
        return new NoCodeParameter(NoCodeDataType.String, "Einheit", "Wählen Sie Sekunden, Minuten, Stunden oder Tage. Ein Tag entspricht immer 24 Stunden.",
                new NoCodeParameterOption("Sekunden", "seconds", "Sekunde"),
                new NoCodeParameterOption("Minuten", "minutes", "Minute"),
                new NoCodeParameterOption("Stunden", "hours", "Stunde"),
                new NoCodeParameterOption("Tage", "days", "Tag")).withSentence("in der Einheit");
    }

    @Nonnull
    static BigDecimal factor(@Nonnull String unit) throws NoCodeException {
        return switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "seconds" -> BigDecimal.ONE;
            case "minutes" -> BigDecimal.valueOf(60);
            case "hours" -> BigDecimal.valueOf(3600);
            case "days" -> BigDecimal.valueOf(86400);
            default -> throw new NoCodeException("Bitte wählen Sie Sekunden, Minuten, Stunden oder Tage als Einheit aus.");
        };
    }

    static long seconds(@Nonnull BigDecimal value) throws NoCodeException {
        try {
            return DurationValueUtils.requireSeconds(value);
        } catch (IllegalArgumentException exception) {
            throw new NoCodeException(exception.getMessage());
        }
    }
}
