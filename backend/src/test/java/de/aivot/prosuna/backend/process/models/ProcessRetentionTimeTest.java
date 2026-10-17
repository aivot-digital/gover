package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRetentionTimeTest {
    @Test
    void maximumValue_KeepsEveryUnitWithinOneHundredCalendarYears() {
        assertEquals(36_524, ProcessRetentionTime.maximumValue(ProcessRetentionTimeUnit.Days));
        assertEquals(5_217, ProcessRetentionTime.maximumValue(ProcessRetentionTimeUnit.Weeks));
        assertEquals(1_200, ProcessRetentionTime.maximumValue(ProcessRetentionTimeUnit.Months));
        assertEquals(100, ProcessRetentionTime.maximumValue(ProcessRetentionTimeUnit.Years));

        var previousZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("UTC"));
        try {
            var leapDay = Instant.parse("2000-02-29T12:00:00Z");
            var hundredYears = ProcessRetentionTime.calculate(leapDay, 100, ProcessRetentionTimeUnit.Years);
            for (var unit : ProcessRetentionTimeUnit.values()) {
                assertFalse(ProcessRetentionTime.calculate(leapDay, ProcessRetentionTime.maximumValue(unit), unit)
                        .isAfter(hundredYears));
            }
            assertTrue(ProcessRetentionTime.calculate(leapDay, 36_525, ProcessRetentionTimeUnit.Days)
                    .isAfter(hundredYears));
        } finally {
            ApplicationTimeZone.configure(previousZone);
        }
    }

    @Test
    void calculate_UsesCalendarUnitsInBusinessTimeZone() {
        var previousZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
        try {
            var beforeDaylightSaving = Instant.parse("2026-03-28T11:00:00Z");
            assertEquals(Instant.parse("2026-03-29T10:00:00Z"),
                    ProcessRetentionTime.calculate(beforeDaylightSaving, 1, ProcessRetentionTimeUnit.Days));
            assertEquals(Instant.parse("2026-04-04T10:00:00Z"),
                    ProcessRetentionTime.calculate(beforeDaylightSaving, 1, ProcessRetentionTimeUnit.Weeks));

            var endOfJanuary = Instant.parse("2026-01-31T11:00:00Z");
            assertEquals(Instant.parse("2026-02-28T11:00:00Z"),
                    ProcessRetentionTime.calculate(endOfJanuary, 1, ProcessRetentionTimeUnit.Months));
            assertEquals(Instant.parse("2027-01-31T11:00:00Z"),
                    ProcessRetentionTime.calculate(endOfJanuary, 1, ProcessRetentionTimeUnit.Years));
        } finally {
            ApplicationTimeZone.configure(previousZone);
        }
    }

    @Test
    void calculate_RejectsNonPositiveDurations() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessRetentionTime.calculate(Instant.now(), 0, ProcessRetentionTimeUnit.Days));
    }
}
