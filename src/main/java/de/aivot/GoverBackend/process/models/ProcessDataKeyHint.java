package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;

public record ProcessDataKeyHint(
        @Nonnull
        String key,
        @Nonnull
        ProcessDataKeyHintType type
) {
}
