package de.aivot.GoverBackend.userRoles.models;

import de.aivot.GoverBackend.userRoles.enums.DepartmentPermission;
import de.aivot.GoverBackend.userRoles.enums.FormRolePermission;
import jakarta.annotation.Nullable;

public class RoleDefinition {
    @Nullable
    private String name;
    @Nullable
    private String description;
    @Nullable
    private DepartmentPermission departmentPermission;
    @Nullable
    private FormRolePermission formPermission;

    @Nullable
    public String getName() {
        return name;
    }

    public RoleDefinition setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public RoleDefinition setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nullable
    public FormRolePermission getFormPermission() {
        return formPermission;
    }

    public RoleDefinition setFormPermission(@Nullable FormRolePermission formPermission) {
        this.formPermission = formPermission;
        return this;
    }

    @Nullable
    public DepartmentPermission getDepartmentPermission() {
        return departmentPermission;
    }

    public RoleDefinition setDepartmentPermission(@Nullable DepartmentPermission departmentPermission) {
        this.departmentPermission = departmentPermission;
        return this;
    }
}
