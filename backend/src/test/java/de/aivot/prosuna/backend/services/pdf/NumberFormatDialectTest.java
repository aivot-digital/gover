package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberFormatDialectTest {
    private final ZoneId originalZoneId = ApplicationTimeZone.getZoneId();

    @AfterEach
    void restoreApplicationTimeZone() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void formatInstant_UsesApplicationTimeZoneForSummerAndWinter() {
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
        var dialect = new NumberFormatDialect();

        assertEquals(
                "29.07.2026 09:00",
                dialect.formatInstant(Instant.parse("2026-07-29T07:00:00Z"), "dd.MM.yyyy HH:mm")
        );
        assertEquals(
                "29.01.2026 09:00",
                dialect.formatInstant("2026-01-29T08:00:00Z", "dd.MM.yyyy HH:mm")
        );
    }

    @Test
    void formatInstant_DoesNotInterpretOffsetlessStrings() {
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
        var value = "2026-07-29T09:00:00";

        assertEquals(value, new NumberFormatDialect().formatInstant(value, "dd.MM.yyyy HH:mm"));
    }
}
