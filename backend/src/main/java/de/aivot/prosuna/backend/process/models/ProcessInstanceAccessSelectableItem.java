package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ProcessInstanceAccessSelectableItem(
        @Nonnull String type,
        @Nonnull String id,
        @Nonnull String label,
        @Nullable String subLabel,
        @Nullable Integer departmentDepth,
        @Nullable Integer eligibleUserCount
) {
    public ProcessInstanceAccessSelectableItem(@Nonnull String type, @Nonnull String id) {
        this(type, id, id, null, null, null);
    }

    public ProcessInstanceAccessSelectableItem(
            @Nonnull String type,
            @Nonnull String id,
            @Nonnull String label,
            @Nullable String subLabel,
            @Nullable Integer departmentDepth
    ) {
        this(type, id, label, subLabel, departmentDepth, null);
    }
}
