package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.util.UUID;

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
    @NotNull(message = "Der interne Titel der Prozessdefinition darf nicht null sein.")
    @NotBlank(message = "Der interne Titel der Prozessdefinition darf nicht leer sein.")
    @Length(min=3, max = 96, message = "Der interne Titel der Prozessdefinition muss zwischen 3 und 96 Zeichen lang sein.")
    private String internalTitle;

    @Nonnull
    @NotNull(message = "Die ID der Organisationseinheit darf nicht null sein.")
    private Integer departmentId;

    @Nonnull
    @NotNull(message = "Die ID der Organisationseinheit darf nicht null sein.")
    private UUID accessKey;

    @Nonnull
    @NotBlank(message = "Der URL-Namespace des Prozesses darf nicht leer sein.")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Der URL-Namespace des Prozesses darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.")
    @Length(min = 3, max = 128, message = "Der URL-Namespace des Prozesses muss zwischen 3 und 128 Zeichen lang sein.")
    private String slug;

    @Nonnull
    @NotNull(message = "Die Versionsanzahl darf nicht null sein.")
    private Integer versionCount;

    @Nullable
    private Integer draftedVersion;

    @Nullable
    private Integer publishedVersion;

    @Nonnull
    @NotNull(message = "Das Erstellungsdatum darf nicht null sein.")
    private Instant created;

    @Nonnull
    @NotNull(message = "Das Aktualisierungsdatum darf nicht null sein.")
    private Instant updated;

    // region Constructors

    // Empty constructor for JPA
    public ProcessEntity() {

    }

    // Full constructor
    public ProcessEntity(@Nonnull Integer id,
                         @Nonnull String internalTitle,
                         @Nonnull Integer departmentId,
                         @Nonnull UUID accessKey,
                         @Nonnull String slug,
                         @Nonnull Integer versionCount,
                         @Nullable Integer draftedVersion,
                         @Nullable Integer publishedVersion,
                         @Nonnull Instant created,
                         @Nonnull Instant updated) {
        this.id = id;
        this.internalTitle = internalTitle;
        this.departmentId = departmentId;
        this.accessKey = accessKey;
        this.slug = slug;
        this.versionCount = versionCount;
        this.draftedVersion = draftedVersion;
        this.publishedVersion = publishedVersion;
        this.created = created;
        this.updated = updated;
    }

    public ProcessEntity(@Nonnull Integer id,
                         @Nonnull String internalTitle,
                         @Nonnull Integer departmentId,
                         @Nonnull UUID accessKey,
                         @Nonnull Integer versionCount,
                         @Nullable Integer draftedVersion,
                         @Nullable Integer publishedVersion,
                         @Nonnull Instant created,
                         @Nonnull Instant updated) {
        this(
                id,
                internalTitle,
                departmentId,
                accessKey,
                "process-" + id,
                versionCount,
                draftedVersion,
                publishedVersion,
                created,
                updated
        );
    }

    // endregion

    // region Signals

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
    public Instant getCreated() {
        return created;
    }

    public ProcessEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessEntity setAccessKey(@Nonnull UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    @Nonnull
    public String getSlug() {
        return slug;
    }

    public ProcessEntity setSlug(@Nonnull String slug) {
        this.slug = slug;
        return this;
    }

    // endregion
}
