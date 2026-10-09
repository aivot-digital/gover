package de.aivot.prosuna.backend.process.projections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ProcessTaskRestartProjection(
        @Nonnull Long id,
        @Nullable Long restartForTaskId
) {
}
