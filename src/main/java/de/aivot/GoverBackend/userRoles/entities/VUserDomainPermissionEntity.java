package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @deprecated use VUserDepartmentPermissionEntity and VUserTeamPermissionEntity instead
 */
@Deprecated
@Entity
@Table(name = "v_user_domain_permission")
public class VUserDomainPermissionEntity {
    @Id
    @Nonnull
    private String userId;

    @Nullable
    private Integer departmentId;

    @Nullable
    private Integer teamId;

    @Nonnull
    private List<String> permissions;

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VUserDomainPermissionEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VUserDomainPermissionEntity setDepartmentId(@Nullable Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nullable
    public Integer getTeamId() {
        return teamId;
    }

    public VUserDomainPermissionEntity setTeamId(@Nullable Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public VUserDomainPermissionEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
