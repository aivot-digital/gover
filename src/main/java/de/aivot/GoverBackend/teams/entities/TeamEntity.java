package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

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
    public LocalDateTime getCreated() {
        return created;
    }

    public TeamEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public TeamEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}

