package de.aivot.GoverBackend.department.entities;

import de.aivot.GoverBackend.enums.UserRole;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "organizational_unit_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organizationalUnitId", "userId"})
})
public class DepartmentMembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_memberships_id_seq")
    @SequenceGenerator(name = "department_memberships_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    private Integer organizationalUnitId;

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

    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public void setOrganizationalUnitId(Integer departmentId) {
        this.organizationalUnitId = departmentId;
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
