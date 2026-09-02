package de.aivot.prosuna.backend.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void parseIsoInstantShouldKeepExplicitUtcAndOffsetBehavior() {
        assertEquals(
                Instant.parse("2026-06-15T07:30:00Z"),
                IsoTimestampUtils.parseIsoInstant("2026-06-15T07:30:00Z")
        );
        assertEquals(
                Instant.parse("2026-06-15T07:30:00Z"),
                IsoTimestampUtils.parseIsoInstant("2026-06-15T10:30:00+03:00")
        );
    }

    @Test
    void parseIsoInstantShouldInterpretOffsetlessTimestampInApplicationTimeZone() {
        assertEquals(
                Instant.parse("2026-07-29T07:00:00Z"),
                IsoTimestampUtils.parseIsoInstant("2026-07-29T09:00:00")
        );
    }

    @Test
    void parseIsoInstantShouldPreserveOffsetlessFractionalSeconds() {
        assertEquals(
                Instant.parse("2026-07-29T07:00:00.123456789Z"),
                IsoTimestampUtils.parseIsoInstant("2026-07-29T09:00:00.123456789")
        );
    }

    @Test
    void parseIsoInstantShouldRejectOffsetlessTimestampDuringDstGap() {
        assertThrows(
                DateTimeParseException.class,
                () -> IsoTimestampUtils.parseIsoInstant("2026-03-29T02:30:00")
        );
    }

    @Test
    void parseIsoInstantShouldUseEarlierOffsetDuringDstOverlap() {
        assertEquals(
                Instant.parse("2026-10-25T00:30:00Z"),
                IsoTimestampUtils.parseIsoInstant("2026-10-25T02:30:00")
        );
    }

    @Test
    void parseIsoInstantShouldRejectTimestampsWithoutSeconds() {
        assertThrows(
                DateTimeParseException.class,
                () -> IsoTimestampUtils.parseIsoInstant("2026-06-15T10:30+03:00")
        );
        assertThrows(
                DateTimeParseException.class,
                () -> IsoTimestampUtils.parseIsoInstant("2026-06-15T10:30")
        );
    }

    @Test
    void parseIsoInstantShouldRejectNonCanonicalEndOfDayNotation() {
        assertThrows(
                DateTimeParseException.class,
                () -> IsoTimestampUtils.parseIsoInstant("2026-06-15T24:00:00+02:00")
        );
        assertThrows(
                DateTimeParseException.class,
                () -> IsoTimestampUtils.parseIsoInstant("2026-06-15T24:00:00")
        );
    }
}
