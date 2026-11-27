package de.aivot.prosuna.backend.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilsTest {
    @Test
    void roundHalfUpRoundsNumbersWithinTheDigitLimit() {
        assertEquals(new BigDecimal("1.24"), NumberUtils.roundHalfUp(new BigDecimal("1.235"), 2));
        assertEquals(new BigDecimal("1e99").setScale(2), NumberUtils.roundHalfUp(new BigDecimal("1e99"), 2));
        assertEquals(new BigDecimal("0.01"), NumberUtils.roundHalfUp(new BigDecimal("0.005"), 2));
        assertEquals(new BigDecimal("0.00"), NumberUtils.roundHalfUp(new BigDecimal("0.0049"), 2));
    }

    @Test
    void roundHalfUpRejectsEnormousNumbersAndRoundsTinyNumbersToZeroWithoutExpandingThem() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertNull(NumberUtils.roundHalfUp(new BigDecimal("1e101"), 2));
            assertNull(NumberUtils.roundHalfUp(new BigDecimal("-1e300000000"), 8));
            assertNull(NumberUtils.roundHalfUp(new BigDecimal("1e2147483647"), 0));
            assertEquals(new BigDecimal("0.00"), NumberUtils.roundHalfUp(new BigDecimal("1e-300000000"), 2));
            assertEquals(BigDecimal.ZERO, NumberUtils.roundHalfUp(new BigDecimal("-1e-999999999"), 0));
        });
    }

    @Test
    void parseGermanNumberRejectsEnormousExponents() {
        assertEquals(new BigDecimal("1234.57"), NumberUtils.parseGermanNumber("1.234,567", 2));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertNull(NumberUtils.parseGermanNumber("1,5e300000000", 2)));
    }

    @Test
    void exceedsDigitLimitChecksBothSidesOfTheDecimalPoint() {
        assertFalse(NumberUtils.exceedsDigitLimit(new BigDecimal("1e99")));
        assertFalse(NumberUtils.exceedsDigitLimit(new BigDecimal("1e-100")));
        assertTrue(NumberUtils.exceedsDigitLimit(new BigDecimal("1e100")));
        assertTrue(NumberUtils.exceedsDigitLimit(new BigDecimal("1e-101")));
        // Precision minus scale overflows int for this exponent.
        assertTrue(NumberUtils.exceedsDigitLimit(new BigDecimal("1e2147483647")));
    }
}
