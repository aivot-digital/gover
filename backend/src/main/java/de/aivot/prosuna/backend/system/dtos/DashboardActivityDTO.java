package de.aivot.prosuna.backend.system.dtos;

import de.aivot.prosuna.backend.system.enums.DashboardActivityPeriod;
import jakarta.annotation.Nonnull;

import java.time.LocalDate;
import java.util.List;

public record DashboardActivityDTO(
        boolean available,
        @Nonnull DashboardActivityPeriod period,
        long started,
        long completed,
        long active,
        @Nonnull List<Bucket> buckets
) {
    public record Bucket(
            @Nonnull LocalDate periodStart,
            long started,
            long completed
    ) {
    }
}
