package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

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

    @Nullable
    private String caseNumberTemplate;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    @Nullable
    private Instant published;

    @Nullable
    private Instant revoked;

    // region Constructors

    // Empty constructor for JPA
    public ProcessVersionEntity() {

    }

    public ProcessVersionEntity(@Nonnull Integer processId,
                                @Nonnull Integer processVersion,
                                @Nonnull ProcessVersionStatus status,
                                @Nonnull String publicTitle,
                                @Nullable String caseNumberTemplate,
                                @Nonnull Instant created,
                                @Nonnull Instant updated,
                                @Nullable Instant published,
                                @Nullable Instant revoked) {
        this.processId = processId;
        this.processVersion = processVersion;
        this.status = status;
        this.publicTitle = publicTitle;
        this.caseNumberTemplate = caseNumberTemplate;
        this.created = created;
        this.updated = updated;
        this.published = published;
        this.revoked = revoked;
    }

    // endregion

    // region Signale

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
    public Instant getCreated() {
        return created;
    }

    public ProcessVersionEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessVersionEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Instant getPublished() {
        return published;
    }

    public ProcessVersionEntity setPublished(@Nullable Instant published) {
        this.published = published;
        return this;
    }

    @Nullable
    public Instant getRevoked() {
        return revoked;
    }

    public ProcessVersionEntity setRevoked(@Nullable Instant revoked) {
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

    @Nullable
    public String getCaseNumberTemplate() {
        return caseNumberTemplate;
    }

    public ProcessVersionEntity setCaseNumberTemplate(@Nullable String caseNumberTemplate) {
        this.caseNumberTemplate = caseNumberTemplate;
        return this;
    }

    // endregion
}
