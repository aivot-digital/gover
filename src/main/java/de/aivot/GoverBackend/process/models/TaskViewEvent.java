package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;

public record TaskViewEvent(
        @Nonnull
        String label,
        @Nonnull
        String event
) {
}
