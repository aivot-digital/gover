package de.aivot.GoverBackend.permissions.entities;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public class VUserDepartmentPermissionEntityId {
    private Integer userId;
    private Integer departmentId;

    public static VUserDepartmentPermissionEntityId of(@Nonnull Integer userId,
                                                       @Nonnull Integer departmentId) {
        VUserDepartmentPermissionEntityId id = new VUserDepartmentPermissionEntityId();
        id.userId = userId;
        id.departmentId = departmentId;
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserDepartmentPermissionEntityId that = (VUserDepartmentPermissionEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(departmentId, that.departmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, departmentId);
    }

    public Integer getUserId() {
        return userId;
    }

    public VUserDepartmentPermissionEntityId setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VUserDepartmentPermissionEntityId setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }
}
