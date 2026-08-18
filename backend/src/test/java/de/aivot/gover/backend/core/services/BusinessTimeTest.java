package de.aivot.gover.backend.core.services;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessTimeTest {
    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("Europe/Berlin");

    @Test
    void shouldProvideCurrentBusinessDateAndTimeUsingTheInjectedClock() {
        var instant = Instant.parse("2026-07-29T22:30:00Z");
        var clock = Clock.fixed(instant, ZoneId.of("America/Los_Angeles"));
        var businessTime = new BusinessTime(APPLICATION_TIME_ZONE, clock);

        assertEquals(instant, businessTime.now());
        assertEquals(LocalDate.of(2026, 7, 30), businessTime.today());
        assertEquals(APPLICATION_TIME_ZONE, businessTime.zonedNow().getZone());
        assertEquals(LocalDateTime.of(2026, 7, 30, 0, 30), businessTime.zonedNow().toLocalDateTime());
    }

    @Test
    void shouldResolveRegularLocalDateTimeInApplicationTimeZone() {
        var businessTime = businessTime();

        var result = businessTime.resolve(LocalDateTime.of(2026, 7, 29, 9, 0));

        assertEquals(Instant.parse("2026-07-29T07:00:00Z"), result);
    }

    @Test
    void shouldRejectNonexistentLocalDateTimeDuringDstGap() {
        var businessTime = businessTime();

        assertThrows(
                DateTimeException.class,
                () -> businessTime.resolve(LocalDateTime.of(2026, 3, 29, 2, 30))
        );
    }

    @Test
    void shouldUseEarlierExecutionDuringDstOverlap() {
        var businessTime = businessTime();

        var result = businessTime.resolve(LocalDateTime.of(2026, 10, 25, 2, 30));

        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), result);
    }

    private BusinessTime businessTime() {
        return new BusinessTime(APPLICATION_TIME_ZONE, Clock.systemUTC());
    }
}
