package de.aivot.gover.backend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "teams")
public class TeamEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "teams_id_seq")
    @SequenceGenerator(name = "teams_id_seq", allocationSize = 1)
    private Integer id;

    @Nullable
    @Column(length = 64, nullable = false, unique = true)
    @NotNull(message = "Der Name des Teams darf nicht null sein.")
    @NotBlank(message = "Der Name des Teams darf nicht leer sein.")
    @Size(min = 3, max = 64, message = "Der Name des Teams muss zwischen 3 und 64 Zeichen lang sein.")
    private String name;

    @Nonnull
    @Column(nullable = false)
    private Instant created;

    @Nonnull
    @Column(nullable = false)
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

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public TeamEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public TeamEntity setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public TeamEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public TeamEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}

