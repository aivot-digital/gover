package de.aivot.GoverBackend.permissions.models;

import jakarta.annotation.Nonnull;

public record PermissionEntry(
        @Nonnull
        String permission,
        @Nonnull
        String label,
        @Nonnull
        String description
) {
    public static PermissionEntry of(
            @Nonnull String permission,
            @Nonnull String label,
            @Nonnull String description
    ) {
        return new PermissionEntry(permission, label, description);
    }
}
