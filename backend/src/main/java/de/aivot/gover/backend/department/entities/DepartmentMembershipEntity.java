package de.aivot.gover.backend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_memberships_id_seq")
    @SequenceGenerator(name = "department_memberships_id_seq", allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der Organisationseinheit darf nicht null sein")
    private Integer departmentId;

    @Nonnull
    @NotNull(message = "Die ID des Benutzers darf nicht null sein")
    private String userId;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    // region Signales

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
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
    public Instant getCreated() {
        return created;
    }

    public DepartmentMembershipEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public DepartmentMembershipEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
