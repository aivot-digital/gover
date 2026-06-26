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

    @Nullable
    private Boolean isDirectMember;

    @Nullable
    private Boolean isIndirectMember;

    @Nullable
    private List<String> directSystemRolePermissions;

    @Nullable
    private List<String> indirectSystemRolePermissions;

    @Nonnull
    private List<String> systemRolePermissions;

    @Nonnull
    private List<String> systemRoleNames;

    @Nonnull
    private List<Integer> systemRoleIds;

    @Nullable
    private List<String> directDomainRolePermissions;

    @Nullable
    private List<String> indirectDomainRolePermissions;

    @Nonnull
    private List<String> domainRolePermissions;

    @Nonnull
    private List<String> domainRoleNames;

    @Nonnull
    private List<Integer> domainRoleIds;

    @Nullable
    private List<String> directPermissions;

    @Nullable
    private List<String> indirectPermissions;

    @Nullable
    private List<String> deputyForUserIds;

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

    @Nullable
    public Boolean getIsDirectMember() {
        return isDirectMember;
    }

    public VUserDomainPermissionEntity setIsDirectMember(@Nullable Boolean isDirectMember) {
        this.isDirectMember = isDirectMember;
        return this;
    }

    @Nullable
    public Boolean getIsIndirectMember() {
        return isIndirectMember;
    }

    public VUserDomainPermissionEntity setIsIndirectMember(@Nullable Boolean isIndirectMember) {
        this.isIndirectMember = isIndirectMember;
        return this;
    }

    @Nullable
    public List<String> getDirectSystemRolePermissions() {
        return directSystemRolePermissions;
    }

    public VUserDomainPermissionEntity setDirectSystemRolePermissions(@Nullable List<String> directSystemRolePermissions) {
        this.directSystemRolePermissions = directSystemRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectSystemRolePermissions() {
        return indirectSystemRolePermissions;
    }

    public VUserDomainPermissionEntity setIndirectSystemRolePermissions(@Nullable List<String> indirectSystemRolePermissions) {
        this.indirectSystemRolePermissions = indirectSystemRolePermissions;
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

    @Nullable
    public List<String> getDirectDomainRolePermissions() {
        return directDomainRolePermissions;
    }

    public VUserDomainPermissionEntity setDirectDomainRolePermissions(@Nullable List<String> directDomainRolePermissions) {
        this.directDomainRolePermissions = directDomainRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectDomainRolePermissions() {
        return indirectDomainRolePermissions;
    }

    public VUserDomainPermissionEntity setIndirectDomainRolePermissions(@Nullable List<String> indirectDomainRolePermissions) {
        this.indirectDomainRolePermissions = indirectDomainRolePermissions;
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

    @Nullable
    public List<String> getDirectPermissions() {
        return directPermissions;
    }

    public VUserDomainPermissionEntity setDirectPermissions(@Nullable List<String> directPermissions) {
        this.directPermissions = directPermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectPermissions() {
        return indirectPermissions;
    }

    public VUserDomainPermissionEntity setIndirectPermissions(@Nullable List<String> indirectPermissions) {
        this.indirectPermissions = indirectPermissions;
        return this;
    }

    @Nullable
    public List<String> getDeputyForUserIds() {
        return deputyForUserIds;
    }

    public VUserDomainPermissionEntity setDeputyForUserIds(@Nullable List<String> deputyForUserIds) {
        this.deputyForUserIds = deputyForUserIds;
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
