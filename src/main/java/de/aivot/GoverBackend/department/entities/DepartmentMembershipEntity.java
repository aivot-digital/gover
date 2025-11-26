package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "department_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "departmentId",
                "userId",
        })
})
public class DepartmentMembershipEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der Organisationseinheit darf nicht null sein")
    private Integer departmentId;

    @Nonnull
    @NotNull(message = "Die ID des Benutzers darf nicht null sein")
    private String userId;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    // region Signales

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public DepartmentMembershipEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public DepartmentMembershipEntity setDepartmentId(@Nonnull Integer organizationalUnitId) {
        this.departmentId = organizationalUnitId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public DepartmentMembershipEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public DepartmentMembershipEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public DepartmentMembershipEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
