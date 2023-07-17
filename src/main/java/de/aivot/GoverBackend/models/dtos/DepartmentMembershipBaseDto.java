package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;


public abstract class DepartmentMembershipBaseDto {
    private Integer id;

    private UserRole role;

    public DepartmentMembershipBaseDto(DepartmentMembership departmentMembership) {
        id = departmentMembership.getId();
        role = departmentMembership.getRole();
    }

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // endregion
}
