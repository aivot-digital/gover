package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** An empty duration leaves the phase without a deadline. */
public record ProcessNodeDeadlineConfiguration(
        @Nullable
        @Min(value = 1, message = "Die Frist muss mindestens 1 Tag betragen.")
        @Max(value = 3652, message = "Die Frist darf maximal 3652 Tage betragen.")
        Integer days,
        @Nonnull Boolean strict
) {
    public ProcessNodeDeadlineConfiguration {
        strict = Boolean.TRUE.equals(strict);
    }
}
