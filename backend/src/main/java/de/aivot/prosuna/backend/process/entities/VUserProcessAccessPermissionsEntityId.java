package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class VUserProcessAccessPermissionsEntityId {
    @Nonnull
    private String userId;

    @Nullable
    private Integer viaSourceTeamId;

    @Nullable
    private Integer viaSourceDepartmentId;

    @Nonnull
    private Integer targetProcessId;

    public VUserProcessAccessPermissionsEntityId() {
        userId = "";
        targetProcessId = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserProcessAccessPermissionsEntityId that = (VUserProcessAccessPermissionsEntityId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(viaSourceTeamId, that.viaSourceTeamId) &&
                Objects.equals(viaSourceDepartmentId, that.viaSourceDepartmentId) &&
                Objects.equals(targetProcessId, that.targetProcessId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, viaSourceTeamId, viaSourceDepartmentId, targetProcessId);
    }
}
