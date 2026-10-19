package de.aivot.prosuna.backend.user.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

public final class DeputyDateRange {
    private DeputyDateRange() {
    }

    public static boolean isValid(
            @Nonnull LocalDate fromDate,
            @Nullable LocalDate untilDate
    ) {
        return untilDate == null || !untilDate.isBefore(fromDate);
    }

    public static boolean isActive(
            @Nonnull LocalDate fromDate,
            @Nullable LocalDate untilDate,
            @Nonnull LocalDate currentDate
    ) {
        return !currentDate.isBefore(fromDate)
                && (untilDate == null || !currentDate.isAfter(untilDate));
    }
}
