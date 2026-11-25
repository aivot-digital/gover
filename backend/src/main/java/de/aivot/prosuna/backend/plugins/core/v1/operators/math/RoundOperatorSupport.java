package de.aivot.prosuna.backend.plugins.core.v1.operators.math;

import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RoundOperatorSupport {
    // Negative places round to tens, hundreds and so on. More places only cost memory without a practical use.
    private static final int MAX_DECIMAL_PLACES = 15;
    static final NoCodeNumberConstraints DECIMAL_PLACES =
            new NoCodeNumberConstraints(true, (double) -MAX_DECIMAL_PLACES, (double) MAX_DECIMAL_PLACES);

    private RoundOperatorSupport() {
    }

    @Nonnull
    static BigDecimal round(@Nonnull BigDecimal number, int decimalPlaces, @Nonnull RoundingMode tieBreaker) throws NoCodeException {
        if (Math.abs(decimalPlaces) > MAX_DECIMAL_PLACES) {
            throw new NoCodeException("Geben Sie für „Nachkommastellen“ eine ganze Zahl von -%d bis %d ein.".formatted(MAX_DECIMAL_PLACES, MAX_DECIMAL_PLACES));
        }
        // Dropping more digits than the number has always rounds to zero in either nearest-value mode.
        // Computing the power of ten for a large negative scale would be unnecessary.
        if (decimalPlaces < (long) number.scale() - number.precision()) {
            return BigDecimal.ZERO.setScale(Math.max(decimalPlaces, 0));
        }
        var rounded = number.setScale(decimalPlaces, tieBreaker);
        // Rounding to tens or hundreds keeps a negative scale, which text shows as e.g. "2.3E+3" instead of "2300".
        return decimalPlaces < 0 ? rounded.setScale(0) : rounded;
    }
}
