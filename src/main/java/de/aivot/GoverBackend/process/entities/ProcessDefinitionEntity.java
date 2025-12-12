package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_definitions")
public class ProcessDefinitionEntity {
    private static final String ID_SEQUENCE_NAME = "process_definitions_id_seq";
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Name der Prozessdefinition darf nicht null sein.")
    @NotBlank(message = "Der Name der Prozessdefinition darf nicht leer sein.")
    @Length(min=3, max = 96, message = "Der Name der Prozessdefinition muss zwischen 3 und 96 Zeichen lang sein.")
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

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
    public ProcessDefinitionEntity() {

    }

    // Full constructor
    public ProcessDefinitionEntity(@Nonnull Integer id,
                                   @Nonnull String name,
                                   @Nullable String description,
                                   @Nonnull Integer departmentId,
                                   @Nonnull Integer versionCount,
                                   @Nullable Integer draftedVersion,
                                   @Nullable Integer publishedVersion,
                                   @Nonnull LocalDateTime created,
                                   @Nonnull LocalDateTime updated) {
        this.id = id;
        this.name = name;
        this.description = description;
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

    public ProcessDefinitionEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public ProcessDefinitionEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public ProcessDefinitionEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public ProcessDefinitionEntity setDepartmentId(@Nonnull Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nonnull
    public Integer getVersionCount() {
        return versionCount;
    }

    public ProcessDefinitionEntity setVersionCount(@Nonnull Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    @Nullable
    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public ProcessDefinitionEntity setDraftedVersion(@Nullable Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    @Nullable
    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public ProcessDefinitionEntity setPublishedVersion(@Nullable Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessDefinitionEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessDefinitionEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}