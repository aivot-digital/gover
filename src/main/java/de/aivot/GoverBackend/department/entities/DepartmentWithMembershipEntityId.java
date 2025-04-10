package de.aivot.GoverBackend.department.entities;

import java.util.Objects;

public class DepartmentWithMembershipEntityId {
    private Integer id; // The ID of the department
    private Integer membershipId; // The ID of the membership
    private String userId; // The ID of the user

    public DepartmentWithMembershipEntityId() {
    }

    public DepartmentWithMembershipEntityId(Integer id, Integer membershipId, String userId) {
        this.id = id;
        this.membershipId = membershipId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        DepartmentWithMembershipEntityId that = (DepartmentWithMembershipEntityId) object;
        return Objects.equals(id, that.id) && Objects.equals(membershipId, that.membershipId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(membershipId);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public Integer getId() {
        return id;
    }

    public DepartmentWithMembershipEntityId setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getMembershipId() {
        return membershipId;
    }

    public DepartmentWithMembershipEntityId setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DepartmentWithMembershipEntityId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
