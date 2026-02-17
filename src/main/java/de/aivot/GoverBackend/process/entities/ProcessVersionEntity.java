package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_versions")
@IdClass(ProcessVersionEntityId.class)
public class ProcessVersionEntity {
    @Id
    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processId;

    @Id
    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processVersion;

    @Nonnull
    @NotNull(message = "Der Status der Prozessdefinition-Version darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessVersionStatus status;

    @Nonnull
    @NotNull(message = "Der öffentliche Title der Prozessdefinition-Version darf nicht null sein.")
    @NotBlank(message = "Der öffentliche Title der Prozessdefinition-Version darf nicht leer sein.")
    @Length(min=3, max = 96, message = "Der öffentliche Title der Prozessdefinition-Version muss zwischen 3 und 96 Zeichen lang sein.")
    private String publicTitle;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    @Nullable
    private LocalDateTime published;

    @Nullable
    private LocalDateTime revoked;

    // region Constructors

    // Empty constructor for JPA
    public ProcessVersionEntity() {

    }

    public ProcessVersionEntity(@Nonnull Integer processId,
                                @Nonnull Integer processVersion,
                                @Nonnull ProcessVersionStatus status,
                                @Nonnull String publicTitle,
                                @Nonnull LocalDateTime created,
                                @Nonnull LocalDateTime updated,
                                @Nullable LocalDateTime published,
                                @Nullable LocalDateTime revoked) {
        this.processId = processId;
        this.processVersion = processVersion;
        this.status = status;
        this.publicTitle = publicTitle;
        this.created = created;
        this.updated = updated;
        this.published = published;
        this.revoked = revoked;
    }

    // endregion

    // region Signale

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
    public Integer getProcessId() {
        return processId;
    }

    public ProcessVersionEntity setProcessId(@Nonnull Integer processDefinitionId) {
        this.processId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessVersionEntity setProcessVersion(@Nonnull Integer processDefinitionVersion) {
        this.processVersion = processDefinitionVersion;
        return this;
    }

    @Nonnull
    public ProcessVersionStatus getStatus() {
        return status;
    }

    public ProcessVersionEntity setStatus(@Nonnull ProcessVersionStatus status) {
        this.status = status;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessVersionEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessVersionEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getPublished() {
        return published;
    }

    public ProcessVersionEntity setPublished(@Nullable LocalDateTime published) {
        this.published = published;
        return this;
    }

    @Nullable
    public LocalDateTime getRevoked() {
        return revoked;
    }

    public ProcessVersionEntity setRevoked(@Nullable LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    @Nonnull
    public String getPublicTitle() {
        return publicTitle;
    }

    public ProcessVersionEntity setPublicTitle(@Nonnull String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    // endregion
}