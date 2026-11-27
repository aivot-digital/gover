package de.aivot.prosuna.backend.process.projections;

public interface DashboardInstanceCountsProjection {
    long getTotalCount();

    long getOverdueCount();
}
