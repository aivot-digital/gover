package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "process_access_controls")
public class ProcessAccessControlEntity {
    private static final String ID_SEQUENCE_NAME = "process_access_controls_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nullable
    private Integer sourceTeamId;

    @Nullable
    private Integer sourceDepartmentId;

    @Nonnull
    private Integer targetProcessId;

    @Nonnull
    private List<String> permissions;

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

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessAccessControlEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ProcessAccessControlEntity setSourceTeamId(@Nullable Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    @Nullable
    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ProcessAccessControlEntity setSourceDepartmentId(@Nullable Integer sourceDepartmentId) {
        this.sourceDepartmentId = sourceDepartmentId;
        return this;
    }

    @Nonnull
    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    public ProcessAccessControlEntity setTargetProcessId(@Nonnull Integer targetProcessId) {
        this.targetProcessId = targetProcessId;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public ProcessAccessControlEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessAccessControlEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessAccessControlEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
