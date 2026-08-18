package de.aivot.gover.backend.user.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeputyDateRangeTest {
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 7, 30);

    @Test
    void shouldRecognizeFutureAssignment() {
        assertFalse(DeputyDateRange.isActive(
                LocalDate.of(2026, 7, 31),
                null,
                CURRENT_DATE
        ));
    }

    @Test
    void shouldRecognizeActiveAssignment() {
        assertTrue(DeputyDateRange.isActive(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                CURRENT_DATE
        ));
    }

    @Test
    void shouldRecognizeExpiredAssignment() {
        assertFalse(DeputyDateRange.isActive(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 29),
                CURRENT_DATE
        ));
    }

    @Test
    void shouldRecognizeUnlimitedAssignmentOnlyAfterItsStart() {
        assertTrue(DeputyDateRange.isActive(
                LocalDate.of(2026, 7, 30),
                null,
                CURRENT_DATE
        ));
        assertFalse(DeputyDateRange.isActive(
                LocalDate.of(2026, 7, 31),
                null,
                CURRENT_DATE
        ));
    }

    @Test
    void shouldTreatBothBoundariesAsInclusive() {
        assertTrue(DeputyDateRange.isActive(CURRENT_DATE, CURRENT_DATE, CURRENT_DATE));
    }
}
