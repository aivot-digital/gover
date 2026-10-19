package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessTaskDeadlineExceededDataTest {
    private ZoneId previousZone;

    @BeforeEach
    void rememberZone() {
        previousZone = ApplicationTimeZone.getZoneId();
    }

    @AfterEach
    void restoreZone() {
        ApplicationTimeZone.configure(previousZone);
    }

    @ParameterizedTest
    @CsvSource({
            "Europe/Berlin, 2026-10-17T12:30:00Z, 2026-10-17T14:30:00+02:00",
            "Europe/Berlin, 2026-12-17T12:30:00Z, 2026-12-17T13:30:00+01:00",
            "UTC, 2026-10-17T12:30:00Z, 2026-10-17T12:30:00+00:00"
    })
    void toMap_FormatsProcessedAtWithApplicationTimeZoneOffset(String zone, String processedAt, String expected) {
        ApplicationTimeZone.configure(ZoneId.of(zone));
        var deadline = Instant.parse("2026-10-17T10:00:00Z");
        var instant = Instant.parse(processedAt);

        var map = new ProcessTaskDeadlineExceededData("customer", "Antwort", deadline, instant).toMap();

        assertEquals(expected, map.get("processedAt"));
        assertEquals("2026-10-17T10:00:00Z", map.get("deadline"));
        // No-code date operators read element data strings through this parser.
        assertEquals(instant, IsoTimestampUtils.parseIsoInstant((String) map.get("processedAt")));
    }
}
