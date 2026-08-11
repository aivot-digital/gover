package de.aivot.prosuna.backend.process.projections;

import java.time.LocalDate;

public interface DashboardActivityBucketProjection {
    LocalDate getPeriodStart();

    long getStartedCount();

    long getCompletedCount();
}
