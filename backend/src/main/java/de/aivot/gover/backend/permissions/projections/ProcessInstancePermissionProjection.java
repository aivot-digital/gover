package de.aivot.gover.backend.permissions.projections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ProcessInstancePermissionProjection {
    @Nonnull
    String getUserId();

    @Nullable
    Integer getViaSourceTeamId();

    @Nullable
    Integer getViaSourceDepartmentId();

    @Nonnull
    Long getProcessInstanceId();

    @Nonnull
    List<String> getPermissions();
}
