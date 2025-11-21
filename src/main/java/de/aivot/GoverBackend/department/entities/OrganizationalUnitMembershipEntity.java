package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizational_unit_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "organizationalUnitId",
                "userId",
        })
})
public class OrganizationalUnitMembershipEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    private Integer organizationalUnitId;

    @Nonnull
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

    public OrganizationalUnitMembershipEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public OrganizationalUnitMembershipEntity setOrganizationalUnitId(@Nonnull Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public OrganizationalUnitMembershipEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public OrganizationalUnitMembershipEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public OrganizationalUnitMembershipEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
