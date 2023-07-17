package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import de.aivot.GoverBackend.models.entities.User;


public class DepartmentMembershipWithUserDto extends DepartmentMembershipBaseDto {

    private Integer department;

    private User user;

    public DepartmentMembershipWithUserDto(DepartmentMembership departmentMembership) {
        super(departmentMembership);

        department = departmentMembership.getDepartment().getId();
        user = departmentMembership.getUser();
    }

    // region Getters & Setters

    public Integer getDepartment() {
        return department;
    }

    public void setDepartment(Integer department) {
        this.department = department;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    // endregion
}
