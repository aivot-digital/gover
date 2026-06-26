package de.aivot.gover.backend.preset.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "presets")
public class PresetEntity {
    @Id
    @Nonnull
    @Column(columnDefinition = "uuid")
    private UUID key;

    @Nonnull
    @Column(length = 255)
    @Length(min = 3, max = 255)
    private String title;

    @Nullable
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;

    @Nullable
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;

    @Nonnull
    private Integer versionCount;

    @Nullable
    private Instant created;

    @Nullable
    private Instant updated;

    // region Constructors

    // Empty constructor for JPA
    public PresetEntity() {
    }

    // Full constructor
    public PresetEntity(@Nonnull UUID key,
                        @Nonnull String title,
                        @Nullable Integer publishedVersion,
                        @Nullable Integer draftedVersion,
                        @Nonnull Integer versionCount,
                        @Nullable Instant created,
                        @Nullable Instant updated) {
        this.key = key;
        this.title = title;
        this.publishedVersion = publishedVersion;
        this.draftedVersion = draftedVersion;
        this.versionCount = versionCount;
        this.created = created;
        this.updated = updated;
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

    // region Getters & Setters

    @Nonnull
    public UUID getKey() {
        return key;
    }

    public PresetEntity setKey(@Nonnull UUID key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    public PresetEntity setTitle(@Nonnull String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public PresetEntity setPublishedVersion(@Nullable Integer currentStoreVersion) {
        this.publishedVersion = currentStoreVersion;
        return this;
    }

    @Nullable
    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public PresetEntity setDraftedVersion(@Nullable Integer currentPublishedVersion) {
        this.draftedVersion = currentPublishedVersion;
        return this;
    }

    @Nullable
    public Instant getCreated() {
        return created;
    }

    public PresetEntity setCreated(@Nullable Instant created) {
        this.created = created;
        return this;
    }

    @Nullable
    public Instant getUpdated() {
        return updated;
    }

    public PresetEntity setUpdated(@Nullable Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public Integer getVersionCount() {
        return versionCount;
    }

    public PresetEntity setVersionCount(@Nonnull Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    // endregion
}
