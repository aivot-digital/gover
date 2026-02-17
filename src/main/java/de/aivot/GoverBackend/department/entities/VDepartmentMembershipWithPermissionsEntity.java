package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
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
