package de.aivot.GoverBackend.permissions.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;

public class VUserDomainPermissionEntityId {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nullable
    private Integer departmentId;

    @Id
    @Nullable
    private Integer teamId;

    public VUserDomainPermissionEntityId() {
        userId = "";
        departmentId = null;
        teamId = null;
    }

    public VUserDomainPermissionEntityId(@Nonnull String userId,
                                         @Nullable Integer departmentId,
                                         @Nullable Integer teamId) {
        this.userId = userId;
        this.departmentId = departmentId;
        this.teamId = teamId;
    }

    public static VUserDomainPermissionEntityId of(@Nonnull String userId,
                                                   @Nullable Integer departmentId,
                                                   @Nullable Integer teamId) {
        return new VUserDomainPermissionEntityId(userId, departmentId, teamId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserDomainPermissionEntityId that = (VUserDomainPermissionEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(departmentId, that.departmentId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, departmentId, teamId);
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VUserDomainPermissionEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VUserDomainPermissionEntityId setDepartmentId(@Nullable Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nullable
    public Integer getTeamId() {
        return teamId;
    }

    public VUserDomainPermissionEntityId setTeamId(@Nullable Integer teamId) {
        this.teamId = teamId;
        return this;
    }
}
