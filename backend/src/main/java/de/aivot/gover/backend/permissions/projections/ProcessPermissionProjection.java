package de.aivot.gover.backend.permissions.projections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ProcessPermissionProjection {
    @Nonnull
    String getUserId();

    @Nullable
    Integer getViaSourceTeamId();

    @Nullable
    Integer getViaSourceDepartmentId();

    @Nonnull
    Integer getProcessId();

    @Nonnull
    List<String> getPermissions();
}
