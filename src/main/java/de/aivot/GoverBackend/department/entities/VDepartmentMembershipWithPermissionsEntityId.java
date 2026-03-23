package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

public class VDepartmentMembershipWithPermissionsEntityId {
    @Nonnull
    private Integer departmentId;
    @Nonnull
    private String userId;

    // region Constructors

    // Empty constructor for JPA
    public VDepartmentMembershipWithPermissionsEntityId() {
        this.departmentId = 0;
        this.userId = "";
    }

    // Full Constructor
    public VDepartmentMembershipWithPermissionsEntityId(@Nonnull Integer departmentId,
                                                        @Nonnull String userId) {
        this.departmentId = departmentId;
        this.userId = userId;
    }

    // Factory constructor
    public static VDepartmentMembershipWithPermissionsEntityId of(@Nonnull Integer departmentId,
                                                                  @Nonnull String userId) {
        return new VDepartmentMembershipWithPermissionsEntityId(departmentId, userId);
    }

    // endregion

    // region HashCode & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VDepartmentMembershipWithPermissionsEntityId that = (VDepartmentMembershipWithPermissionsEntityId) o;
        return Objects.equals(departmentId, that.departmentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentId, userId);
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithPermissionsEntityId setDepartmentId(@Nonnull Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithPermissionsEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    // endregion
}
