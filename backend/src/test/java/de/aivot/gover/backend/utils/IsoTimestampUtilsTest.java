package de.aivot.gover.backend.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsoTimestampUtilsTest {
    private ZoneId originalZoneId;

    @BeforeEach
    void setUp() {
        originalZoneId = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
    }

    @AfterEach
    void tearDown() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void toOffsetStringShouldUseApplicationTimeZoneByDefault() {
        var result = IsoTimestampUtils.toOffsetString(
                Instant.parse("2026-06-15T07:30:00Z")
        );

        assertEquals("2026-06-15T09:30:00+02:00", result);
    }
}
