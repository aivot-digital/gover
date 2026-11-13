package de.aivot.prosuna.backend.process.projections;

import jakarta.annotation.Nullable;

import java.time.Instant;

public interface ProcessWorkBacklogProjection {
    long getTotal();

    @Nullable
    Instant getOldestCreated();
}
