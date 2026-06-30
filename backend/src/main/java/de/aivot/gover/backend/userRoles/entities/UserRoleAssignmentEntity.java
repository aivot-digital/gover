package de.aivot.gover.backend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "domain_role_assignments")
public class UserRoleAssignmentEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_role_assignments_id_seq")
    @SequenceGenerator(name = "domain_role_assignments_id_seq", allocationSize = 1)
    private Integer id;

    @Nullable
    private Integer departmentMembershipId;

    @Nullable
    private Integer teamMembershipId;

    @Nonnull
    @NotNull(message = "Die ID der Benutzerrollen muss angegeben werden.")
    @Column(name = "domain_role_id")
    private Integer userRoleId;

    @Nonnull
    private Instant created;

    // region Signales

    @PrePersist
    public void prePersist() {
        created = Instant.now();
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public UserRoleAssignmentEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getDepartmentMembershipId() {
        return departmentMembershipId;
    }

    public UserRoleAssignmentEntity setDepartmentMembershipId(@Nullable Integer organizationalUnitMembershipId) {
        this.departmentMembershipId = organizationalUnitMembershipId;
        return this;
    }

    @Nullable
    public Integer getTeamMembershipId() {
        return teamMembershipId;
    }

    public UserRoleAssignmentEntity setTeamMembershipId(@Nullable Integer teamMembershipId) {
        this.teamMembershipId = teamMembershipId;
        return this;
    }

    @Nonnull
    public Integer getUserRoleId() {
        return userRoleId;
    }

    public UserRoleAssignmentEntity setUserRoleId(@Nonnull Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public UserRoleAssignmentEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }


    // endregion
}
