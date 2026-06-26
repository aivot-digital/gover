package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record TaskViewEvent(
        @Nonnull
        String label,
        @Nonnull
        String event,
        @Nullable
        String variant,
        @Nullable
        String color,
        @Nullable
        String alignment
) {
    public TaskViewEvent(@Nonnull String label, @Nonnull String event) {
        this(label, event, null, null, null);
    }

    public TaskViewEvent(@Nonnull String label, @Nonnull String event, @Nullable String variant) {
        this(label, event, variant, null, null);
    }

    public TaskViewEvent(@Nonnull String label, @Nonnull String event, @Nullable String variant, @Nullable String color) {
        this(label, event, variant, color, null);
    }
}
