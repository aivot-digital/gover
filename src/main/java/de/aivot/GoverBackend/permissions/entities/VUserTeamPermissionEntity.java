package de.aivot.GoverBackend.permissions.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "v_user_team_permissions")
@IdClass(VUserTeamPermissionEntityId.class)
public class VUserTeamPermissionEntity {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nonnull
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

    public VUserTeamPermissionEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public Integer getTeamId() {
        return teamId;
    }

    public VUserTeamPermissionEntity setTeamId(@Nonnull Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nullable
    public Boolean getIsDirectMember() {
        return isDirectMember;
    }

    public VUserTeamPermissionEntity setIsDirectMember(@Nullable Boolean isDirectMember) {
        this.isDirectMember = isDirectMember;
        return this;
    }

    @Nullable
    public Boolean getIsIndirectMember() {
        return isIndirectMember;
    }

    public VUserTeamPermissionEntity setIsIndirectMember(@Nullable Boolean isIndirectMember) {
        this.isIndirectMember = isIndirectMember;
        return this;
    }

    @Nullable
    public List<String> getDirectSystemRolePermissions() {
        return directSystemRolePermissions;
    }

    public VUserTeamPermissionEntity setDirectSystemRolePermissions(@Nullable List<String> directSystemRolePermissions) {
        this.directSystemRolePermissions = directSystemRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectSystemRolePermissions() {
        return indirectSystemRolePermissions;
    }

    public VUserTeamPermissionEntity setIndirectSystemRolePermissions(@Nullable List<String> indirectSystemRolePermissions) {
        this.indirectSystemRolePermissions = indirectSystemRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getSystemRolePermissions() {
        return systemRolePermissions;
    }

    public VUserTeamPermissionEntity setSystemRolePermissions(@Nonnull List<String> systemRolePermissions) {
        this.systemRolePermissions = systemRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getSystemRoleNames() {
        return systemRoleNames;
    }

    public VUserTeamPermissionEntity setSystemRoleNames(@Nonnull List<String> systemRoleNames) {
        this.systemRoleNames = systemRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getSystemRoleIds() {
        return systemRoleIds;
    }

    public VUserTeamPermissionEntity setSystemRoleIds(@Nonnull List<Integer> systemRoleIds) {
        this.systemRoleIds = systemRoleIds;
        return this;
    }

    @Nullable
    public List<String> getDirectDomainRolePermissions() {
        return directDomainRolePermissions;
    }

    public VUserTeamPermissionEntity setDirectDomainRolePermissions(@Nullable List<String> directDomainRolePermissions) {
        this.directDomainRolePermissions = directDomainRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectDomainRolePermissions() {
        return indirectDomainRolePermissions;
    }

    public VUserTeamPermissionEntity setIndirectDomainRolePermissions(@Nullable List<String> indirectDomainRolePermissions) {
        this.indirectDomainRolePermissions = indirectDomainRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VUserTeamPermissionEntity setDomainRolePermissions(@Nonnull List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getDomainRoleNames() {
        return domainRoleNames;
    }

    public VUserTeamPermissionEntity setDomainRoleNames(@Nonnull List<String> domainRoleNames) {
        this.domainRoleNames = domainRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getDomainRoleIds() {
        return domainRoleIds;
    }

    public VUserTeamPermissionEntity setDomainRoleIds(@Nonnull List<Integer> domainRoleIds) {
        this.domainRoleIds = domainRoleIds;
        return this;
    }

    @Nullable
    public List<String> getDirectPermissions() {
        return directPermissions;
    }

    public VUserTeamPermissionEntity setDirectPermissions(@Nullable List<String> directPermissions) {
        this.directPermissions = directPermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectPermissions() {
        return indirectPermissions;
    }

    public VUserTeamPermissionEntity setIndirectPermissions(@Nullable List<String> indirectPermissions) {
        this.indirectPermissions = indirectPermissions;
        return this;
    }

    @Nullable
    public List<String> getDeputyForUserIds() {
        return deputyForUserIds;
    }

    public VUserTeamPermissionEntity setDeputyForUserIds(@Nullable List<String> deputyForUserIds) {
        this.deputyForUserIds = deputyForUserIds;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public VUserTeamPermissionEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
