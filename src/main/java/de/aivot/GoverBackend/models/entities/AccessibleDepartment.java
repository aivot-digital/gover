package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.UserRole;
import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Immutable
@Entity(name = "accessible_departments")
public class AccessibleDepartment {
    @Id
    private Integer membershipId;

    private Integer departmentId;

    private Integer userId;

    private UserRole role;

    // region Getters & Setters

    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // endregion
}
