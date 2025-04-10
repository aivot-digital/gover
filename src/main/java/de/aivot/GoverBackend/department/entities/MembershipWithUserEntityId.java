package de.aivot.GoverBackend.department.entities;

import java.io.Serializable;
import java.util.Objects;

public class MembershipWithUserEntityId implements Serializable {
    private Integer id; // The ID of the membership
    private Integer departmentId; // The ID of the department
    private String userId; // The ID of the user

    public MembershipWithUserEntityId() {
    }

    public MembershipWithUserEntityId(Integer id, Integer departmentId, String userId) {
        this.id = id;
        this.departmentId = departmentId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        MembershipWithUserEntityId that = (MembershipWithUserEntityId) object;
        return Objects.equals(id, that.id) && Objects.equals(departmentId, that.departmentId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(departmentId);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public Integer getId() {
        return id;
    }

    public MembershipWithUserEntityId setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public MembershipWithUserEntityId setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public MembershipWithUserEntityId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
