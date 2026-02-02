package de.aivot.GoverBackend.permissions.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "v_user_domain_permissions")
@IdClass(VUserDomainPermissionEntityId.class)
public class VUserDomainPermissionEntity {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nullable
    private Integer departmentId;

    @Id
    @Nullable
    private Integer teamId;

    @Nonnull
    private List<String> systemRolePermissions;

    @Nonnull
    private List<String> systemRoleNames;

    @Nonnull
    private List<Integer> systemRoleIds;

    @Nonnull
    private List<String> domainRolePermissions;

    @Nonnull
    private List<String> domainRoleNames;

    @Nonnull
    private List<Integer> domainRoleIds;

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
    public List<String> getSystemRolePermissions() {
        return systemRolePermissions;
    }

    public VUserDomainPermissionEntity setSystemRolePermissions(@Nonnull List<String> systemRolePermissions) {
        this.systemRolePermissions = systemRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getSystemRoleNames() {
        return systemRoleNames;
    }

    public VUserDomainPermissionEntity setSystemRoleNames(@Nonnull List<String> systemRoleNames) {
        this.systemRoleNames = systemRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getSystemRoleIds() {
        return systemRoleIds;
    }

    public VUserDomainPermissionEntity setSystemRoleIds(@Nonnull List<Integer> systemRoleIds) {
        this.systemRoleIds = systemRoleIds;
        return this;
    }

    @Nonnull
    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VUserDomainPermissionEntity setDomainRolePermissions(@Nonnull List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getDomainRoleNames() {
        return domainRoleNames;
    }

    public VUserDomainPermissionEntity setDomainRoleNames(@Nonnull List<String> domainRoleNames) {
        this.domainRoleNames = domainRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getDomainRoleIds() {
        return domainRoleIds;
    }

    public VUserDomainPermissionEntity setDomainRoleIds(@Nonnull List<Integer> domainRoleIds) {
        this.domainRoleIds = domainRoleIds;
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
