package de.aivot.prosuna.backend.storage.projections;

import jakarta.annotation.Nonnull;

import java.time.Instant;

public record StorageIndexItemStateProjection(
        @Nonnull String pathFromRoot,
        @Nonnull Boolean directory,
        @Nonnull Instant updated
) {
}
