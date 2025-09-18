package de.aivot.GoverBackend.preset.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
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

    @Nullable
    private LocalDateTime created;

    @Nullable
    private LocalDateTime updated;

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
    public LocalDateTime getCreated() {
        return created;
    }

    public PresetEntity setCreated(@Nullable LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdated() {
        return updated;
    }

    public PresetEntity setUpdated(@Nullable LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
