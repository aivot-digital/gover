package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "v_user_department_permissions")
@IdClass(VDepartmentMembershipWithPermissionsEntityId.class)
public class VDepartmentMembershipWithPermissionsEntity {
    @Id
    @Nonnull
    private Integer departmentId;
    @Id
    @Nonnull
    private String userId;

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

    // region Getters & Setters

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithPermissionsEntity setDepartmentId(@Nonnull Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithPermissionsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Boolean getIsDirectMember() {
        return isDirectMember;
    }

    public VDepartmentMembershipWithPermissionsEntity setIsDirectMember(@Nullable Boolean isDirectMember) {
        this.isDirectMember = isDirectMember;
        return this;
    }

    @Nullable
    public Boolean getIsIndirectMember() {
        return isIndirectMember;
    }

    public VDepartmentMembershipWithPermissionsEntity setIsIndirectMember(@Nullable Boolean isIndirectMember) {
        this.isIndirectMember = isIndirectMember;
        return this;
    }

    @Nullable
    public List<String> getDirectSystemRolePermissions() {
        return directSystemRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setDirectSystemRolePermissions(@Nullable List<String> directSystemRolePermissions) {
        this.directSystemRolePermissions = directSystemRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectSystemRolePermissions() {
        return indirectSystemRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setIndirectSystemRolePermissions(@Nullable List<String> indirectSystemRolePermissions) {
        this.indirectSystemRolePermissions = indirectSystemRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getSystemRolePermissions() {
        return systemRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setSystemRolePermissions(@Nonnull List<String> systemRolePermissions) {
        this.systemRolePermissions = systemRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getSystemRoleNames() {
        return systemRoleNames;
    }

    public VDepartmentMembershipWithPermissionsEntity setSystemRoleNames(@Nonnull List<String> systemRoleNames) {
        this.systemRoleNames = systemRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getSystemRoleIds() {
        return systemRoleIds;
    }

    public VDepartmentMembershipWithPermissionsEntity setSystemRoleIds(@Nonnull List<Integer> systemRoleIds) {
        this.systemRoleIds = systemRoleIds;
        return this;
    }

    @Nullable
    public List<String> getDirectDomainRolePermissions() {
        return directDomainRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setDirectDomainRolePermissions(@Nullable List<String> directDomainRolePermissions) {
        this.directDomainRolePermissions = directDomainRolePermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectDomainRolePermissions() {
        return indirectDomainRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setIndirectDomainRolePermissions(@Nullable List<String> indirectDomainRolePermissions) {
        this.indirectDomainRolePermissions = indirectDomainRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getDomainRolePermissions() {
        return domainRolePermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setDomainRolePermissions(@Nonnull List<String> domainRolePermissions) {
        this.domainRolePermissions = domainRolePermissions;
        return this;
    }

    @Nonnull
    public List<String> getDomainRoleNames() {
        return domainRoleNames;
    }

    public VDepartmentMembershipWithPermissionsEntity setDomainRoleNames(@Nonnull List<String> domainRoleNames) {
        this.domainRoleNames = domainRoleNames;
        return this;
    }

    @Nonnull
    public List<Integer> getDomainRoleIds() {
        return domainRoleIds;
    }

    public VDepartmentMembershipWithPermissionsEntity setDomainRoleIds(@Nonnull List<Integer> domainRoleIds) {
        this.domainRoleIds = domainRoleIds;
        return this;
    }

    @Nullable
    public List<String> getDirectPermissions() {
        return directPermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setDirectPermissions(@Nullable List<String> directPermissions) {
        this.directPermissions = directPermissions;
        return this;
    }

    @Nullable
    public List<String> getIndirectPermissions() {
        return indirectPermissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setIndirectPermissions(@Nullable List<String> indirectPermissions) {
        this.indirectPermissions = indirectPermissions;
        return this;
    }

    @Nullable
    public List<String> getDeputyForUserIds() {
        return deputyForUserIds;
    }

    public VDepartmentMembershipWithPermissionsEntity setDeputyForUserIds(@Nullable List<String> deputyForUserIds) {
        this.deputyForUserIds = deputyForUserIds;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public VDepartmentMembershipWithPermissionsEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }

    // endregion
}
