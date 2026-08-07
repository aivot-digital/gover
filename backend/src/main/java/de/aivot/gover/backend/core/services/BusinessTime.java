package de.aivot.gover.backend.core.services;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BusinessTime {
    private final ZoneId zoneId;
    private final Clock clock;

    public BusinessTime(ZoneId zoneId, Clock clock) {
        this.zoneId = zoneId;
        this.clock = clock;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public Clock clock() {
        return clock;
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zoneId));
    }

    public ZonedDateTime zonedNow() {
        return ZonedDateTime.now(clock.withZone(zoneId));
    }

    public Instant resolve(LocalDateTime localDateTime) {
        var validOffsets = zoneId.getRules().getValidOffsets(localDateTime);

        // A local time inside a DST gap has no corresponding instant. Do not let
        // ZonedDateTime silently normalize it into the next valid hour.
        if (validOffsets.isEmpty()) {
            throw new DateTimeException(
                    "Local date-time %s does not exist in timezone %s"
                            .formatted(localDateTime, zoneId)
            );
        }

        // ZoneRules returns the offset before the transition first. During an overlap,
        // this selects the earlier of the two possible executions.
        return localDateTime
                .atOffset(validOffsets.getFirst())
                .toInstant();
    }
}
