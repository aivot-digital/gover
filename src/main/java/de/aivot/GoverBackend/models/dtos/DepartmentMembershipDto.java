package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;


public class DepartmentMembershipDto {
    private Integer id;

    private Integer department;

    private Integer user;

    private UserRole role;

    public static DepartmentMembershipDto valueOf(DepartmentMembership departmentMembership) {
        DepartmentMembershipDto d = new DepartmentMembershipDto();

        d.id = departmentMembership.getId();
        d.department = departmentMembership.getDepartment().getId();
        d.user = departmentMembership.getUser().getId();
        d.role = departmentMembership.getRole();

        return d;
    }

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDepartment() {
        return department;
    }

    public void setDepartment(Integer department) {
        this.department = department;
    }

    public Integer getUser() {
        return user;
    }

    public void setUser(Integer user) {
        this.user = user;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }


    // endregion
}
