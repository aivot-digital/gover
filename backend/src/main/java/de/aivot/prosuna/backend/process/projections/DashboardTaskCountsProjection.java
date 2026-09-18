package de.aivot.prosuna.backend.process.projections;

public interface DashboardTaskCountsProjection {
    long getTotalCount();

    long getOverdueCount();
}
