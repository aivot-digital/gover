package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "processes")
public class ProcessEntity {
    private static final String ID_SEQUENCE_NAME = "processes_id_seq";
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Der interne Title der Prozessdefinition darf nicht null sein.")
    @NotBlank(message = "Der interne Title der Prozessdefinition darf nicht leer sein.")
    @Length(min=3, max = 96, message = "Der interne Title der Prozessdefinition muss zwischen 3 und 96 Zeichen lang sein.")
    private String internalTitle;

    @Nonnull
    @NotNull(message = "Die ID der Organisationseinheit darf nicht null sein.")
    private Integer departmentId;

    @Nonnull
    @NotNull(message = "Die Versionsanzahl darf nicht null sein.")
    private Integer versionCount;

    @Nullable
    private Integer draftedVersion;

    @Nullable
    private Integer publishedVersion;

    @Nonnull
    @NotNull(message = "Das Erstellungsdatum darf nicht null sein.")
    private LocalDateTime created;

    @Nonnull
    @NotNull(message = "Das Aktualisierungsdatum darf nicht null sein.")
    private LocalDateTime updated;

    // region Constructors

    // Empty constructor for JPA
    public ProcessEntity() {

    }

    // Full constructor
    public ProcessEntity(@Nonnull Integer id,
                         @Nonnull String internalTitle,
                         @Nonnull Integer departmentId,
                         @Nonnull Integer versionCount,
                         @Nullable Integer draftedVersion,
                         @Nullable Integer publishedVersion,
                         @Nonnull LocalDateTime created,
                         @Nonnull LocalDateTime updated) {
        this.id = id;
        this.internalTitle = internalTitle;
        this.departmentId = departmentId;
        this.versionCount = versionCount;
        this.draftedVersion = draftedVersion;
        this.publishedVersion = publishedVersion;
        this.created = created;
        this.updated = updated;
    }

    // endregion

    // region Signals

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

    public ProcessEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getInternalTitle() {
        return internalTitle;
    }

    public ProcessEntity setInternalTitle(@Nonnull String name) {
        this.internalTitle = name;
        return this;
    }

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public ProcessEntity setDepartmentId(@Nonnull Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nonnull
    public Integer getVersionCount() {
        return versionCount;
    }

    public ProcessEntity setVersionCount(@Nonnull Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    @Nullable
    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public ProcessEntity setDraftedVersion(@Nullable Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    @Nullable
    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public ProcessEntity setPublishedVersion(@Nullable Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}