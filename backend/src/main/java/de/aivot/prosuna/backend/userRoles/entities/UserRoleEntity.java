package de.aivot.prosuna.backend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import de.aivot.prosuna.backend.lib.models.LockVersioned;

@Entity
@Table(name = "domain_roles")
public class UserRoleEntity implements LockVersioned {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_roles_id_seq")
    @SequenceGenerator(name = "domain_roles_id_seq", allocationSize = 1)
    private Integer id;

    @Nonnull
    @Column(length = 64, nullable = false, unique = true)
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    @Nonnull
    private List<String> permissions;

    @Nonnull
    @Column(nullable = false)
    private Instant created;

    @Nonnull
    @Column(nullable = false)
    private Instant updated;

    @Nullable
    @Version
    @Column(nullable = false)
    private Long lockVersion;

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

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public UserRoleEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public UserRoleEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public UserRoleEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public UserRoleEntity setPermissions(@Nonnull List<String> permission) {
        this.permissions = permission;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public UserRoleEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public UserRoleEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    @Override
    public Long getLockVersion() {
        return lockVersion;
    }

    public UserRoleEntity setLockVersion(@Nullable Long lockVersion) {
        this.lockVersion = lockVersion;
        return this;
    }

    // endregion
}
