package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;

public record ProcessDataKeyHintResponse(
        @Nonnull
        String key,
        @Nonnull
        ProcessDataKeyHintType type,
        @Nonnull
        ProcessNodeEntity node
) {
}
