package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.Locale;

final class DurationOperatorSupport {
    private DurationOperatorSupport() {}

    @Nonnull
    static NoCodeParameter unitParameter() {
        return new NoCodeParameter(NoCodeDataType.String, "Einheit", "Sekunden, Minuten, Stunden oder Tage. Ein Tag entspricht immer 24 Stunden.",
                new NoCodeParameterOption("Sekunden", "seconds"),
                new NoCodeParameterOption("Minuten", "minutes"),
                new NoCodeParameterOption("Stunden", "hours"),
                new NoCodeParameterOption("Tage", "days"));
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
