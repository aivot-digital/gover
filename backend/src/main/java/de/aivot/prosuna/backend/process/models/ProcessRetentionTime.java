package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;

import java.time.Instant;

public final class ProcessRetentionTime {
    private ProcessRetentionTime() {
    }

    public static long maximumValue(@Nonnull ProcessRetentionTimeUnit unit) {
        // Use the shortest possible 100-year calendar span so the limit holds for every start date.
        return switch (unit) {
            case Days -> 36_524;
            case Weeks -> 5_217;
            case Months -> 1_200;
            case Years -> 100;
        };
    }

    @Nonnull
    public static Instant calculate(@Nonnull Instant from, long value, @Nonnull ProcessRetentionTimeUnit unit) {
        if (value <= 0) {
            throw new IllegalArgumentException("The retention time must be positive.");
        }

        // Calendar units follow the configured business time zone, including daylight-saving changes.
        var localTime = from.atZone(ApplicationTimeZone.getZoneId());
        return switch (unit) {
            case Days -> localTime.plusDays(value).toInstant();
            case Weeks -> localTime.plusWeeks(value).toInstant();
            case Months -> localTime.plusMonths(value).toInstant();
            case Years -> localTime.plusYears(value).toInstant();
        };
    }
}
