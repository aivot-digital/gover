package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.UserRole;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "department_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"departmentId", "userId"})
})
public class DepartmentMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_memberships_id_seq")
    @SequenceGenerator(name = "department_memberships_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    private Integer departmentId;

    @NotNull
    private String userId;

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

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
