package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_definition_versions")
@IdClass(ProcessDefinitionVersionEntityId.class)
public class ProcessDefinitionVersionEntity {
    @Id
    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processDefinitionId;

    @Id
    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processDefinitionVersion;

    @Nonnull
    @NotNull(message = "Der Status der Prozessdefinition-Version darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessVersionStatus status;

    @Nonnull
    @NotNull(message = "Die Aufbewahrungseinheit der Prozessdefinition-Version darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessRetentionTimeUnit retentionTimeUnit;

    @Nonnull
    @NotNull(message = "Die Aufbewahrungsdauer der Prozessdefinition-Version darf nicht null sein.")
    private Integer retentionTimeAmount;

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
    public ProcessDefinitionVersionEntity() {

    }

    public ProcessDefinitionVersionEntity(@Nonnull Integer processDefinitionId,
                                          @Nonnull Integer processDefinitionVersion,
                                          @Nonnull ProcessVersionStatus status,
                                          @Nonnull ProcessRetentionTimeUnit retentionTimeUnit,
                                          @Nonnull Integer retentionTimeAmount,
                                          @Nonnull LocalDateTime created,
                                          @Nonnull LocalDateTime updated,
                                          @Nullable LocalDateTime published,
                                          @Nullable LocalDateTime revoked) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionVersion = processDefinitionVersion;
        this.status = status;
        this.retentionTimeUnit = retentionTimeUnit;
        this.retentionTimeAmount = retentionTimeAmount;
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
    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionVersionEntity setProcessDefinitionId(@Nonnull Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionVersionEntity setProcessDefinitionVersion(@Nonnull Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Nonnull
    public ProcessVersionStatus getStatus() {
        return status;
    }

    public ProcessDefinitionVersionEntity setStatus(@Nonnull ProcessVersionStatus status) {
        this.status = status;
        return this;
    }

    @Nonnull
    public ProcessRetentionTimeUnit getRetentionTimeUnit() {
        return retentionTimeUnit;
    }

    public ProcessDefinitionVersionEntity setRetentionTimeUnit(@Nonnull ProcessRetentionTimeUnit retentionTimeUnit) {
        this.retentionTimeUnit = retentionTimeUnit;
        return this;
    }

    @Nonnull
    public Integer getRetentionTimeAmount() {
        return retentionTimeAmount;
    }

    public ProcessDefinitionVersionEntity setRetentionTimeAmount(@Nonnull Integer retentionTimeAmount) {
        this.retentionTimeAmount = retentionTimeAmount;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessDefinitionVersionEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessDefinitionVersionEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getPublished() {
        return published;
    }

    public ProcessDefinitionVersionEntity setPublished(@Nullable LocalDateTime published) {
        this.published = published;
        return this;
    }

    @Nullable
    public LocalDateTime getRevoked() {
        return revoked;
    }

    public ProcessDefinitionVersionEntity setRevoked(@Nullable LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }


    // endregion
}