package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;


public class DepartmentMembershipWithDepartmentDto extends DepartmentMembershipBaseDto {

    private Department department;

    private Integer user;

    public DepartmentMembershipWithDepartmentDto() {
    }

    public DepartmentMembershipWithDepartmentDto(DepartmentMembership departmentMembership) {
        super(departmentMembership);

        department = departmentMembership.getDepartment();
        user = departmentMembership.getUser().getId();
    }

    // region Getters & Setters

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
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
