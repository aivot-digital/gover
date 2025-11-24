package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
public class TeamEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nullable
    @Column(length = 64, nullable = false, unique = true)
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

