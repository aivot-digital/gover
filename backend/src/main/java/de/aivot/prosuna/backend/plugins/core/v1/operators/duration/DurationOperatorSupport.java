package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
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
        if (value == null || value instanceof String text && text.isBlank()) return BigDecimal.ZERO;

        // Not rounded, so a fraction of a second fails the check for whole seconds.
        var number = NoCodeOperator.parseNumber(value);
        if (number == null) {
            throw new NoCodeException("Geben Sie für „" + label + "“ eine Zahl ein.");
        }
        return number;
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
