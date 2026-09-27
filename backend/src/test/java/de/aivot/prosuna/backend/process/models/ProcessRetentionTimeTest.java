package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessRetentionTimeTest {
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
