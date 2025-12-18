package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "system_roles")
public class SystemRoleEntity {
    private static final String ID_SEQUENCE_NAME = "system_roles_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Name der Systemrolle darf nicht null sein.")
    @NotBlank(message = "Der Name der Systemrolle darf nicht leer sein.")
    @Size(min = 3, max = 64, message = "Der Name der Systemrolle muss zwischen 3 und 64 Zeichen lang sein.")
    @Column(length = 64, nullable = false, unique = true)
    private String name;

    @Nullable
    @Size(max = 512, message = "Die Beschreibung der Systemrolle darf maximal 512 Zeichen lang sein.")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Nonnull
    @Column(nullable = false)
    @NotNull(message = "Die Berechtigungen der Systemrolle dürfen nicht null sein.")
    private List<String> permissions;

    @Nonnull
    @Column(nullable = false)
    private LocalDateTime created;

    @Nonnull
    @Column(nullable = false)
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

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public SystemRoleEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public SystemRoleEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public SystemRoleEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public SystemRoleEntity setPermissions(@Nonnull List<String> permission) {
        this.permissions = permission;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public SystemRoleEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public SystemRoleEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
