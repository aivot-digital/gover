package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.DepartmentMembership;


public class DepartmentMembershipDto extends DepartmentMembershipBaseDto {

    private Integer department;

    private Integer user;

    public DepartmentMembershipDto() {
    }

    public DepartmentMembershipDto(DepartmentMembership departmentMembership) {
        super(departmentMembership);

        department = departmentMembership.getDepartment().getId();
        user = departmentMembership.getUser().getId();
    }

    // region Getters & Setters

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


    // endregion
}
