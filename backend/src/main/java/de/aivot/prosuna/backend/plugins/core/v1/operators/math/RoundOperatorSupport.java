package de.aivot.prosuna.backend.plugins.core.v1.operators.math;

import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RoundOperatorSupport {
    static final NoCodeNumberConstraints DECIMAL_PLACES =
            new NoCodeNumberConstraints(true, (double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE);

    private RoundOperatorSupport() {
    }

    @Nonnull
    static BigDecimal round(@Nonnull BigDecimal number, int decimalPlaces, @Nonnull RoundingMode tieBreaker) {
        // Dropping more digits than the number has always rounds to zero in either nearest-value mode.
        // Computing the power of ten for a large negative scale would be unnecessary.
        if (decimalPlaces < (long) number.scale() - number.precision()) {
            return BigDecimal.ZERO.setScale(decimalPlaces);
        }
        return number.setScale(decimalPlaces, tieBreaker);
    }
}
