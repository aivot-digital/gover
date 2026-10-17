package de.aivot.prosuna.backend.permissions.projections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface DomainPermissionProjection {
    @Nonnull
    String getUserId();

    @Nullable
    Integer getDepartmentId();

    @Nullable
    Integer getTeamId();

    @Nonnull
    List<String> getPermissions();
}
