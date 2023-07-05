package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.UserRole;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "department_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"department_id", "user_id"})
})
public class DepartmentMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_memberships_id_seq")
    @SequenceGenerator(name = "department_memberships_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne
    private Department department;

    @NotNull
    @ManyToOne
    private User user;

    @NotNull
    @ColumnDefault("0")
    private UserRole role;

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
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
