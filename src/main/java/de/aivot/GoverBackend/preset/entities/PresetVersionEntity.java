package de.aivot.GoverBackend.preset.entities;

import de.aivot.GoverBackend.core.converters.GroupLayoutConverter;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.form.enums.FormStatus;
import jakarta.persistence.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(PresetVersionEntityId.class)
@Table(name = "preset_versions")
public class PresetVersionEntity {
    @Id
    @Nonnull
    @Column(columnDefinition = "uuid")
    private UUID presetKey;

    @Id
    @Nonnull
    @Column(columnDefinition = "int2")
    private Integer version;

    @Nonnull
    @Convert(converter = GroupLayoutConverter.class)
    @Column(columnDefinition = "jsonb")
    private GroupLayout rootElement;

    @Nonnull
    private FormStatus status;

    @Nullable
    private LocalDateTime created;

    @Nullable
    private LocalDateTime updated;

    @Nullable
    private LocalDateTime published;

    @Nullable
    private LocalDateTime revoked;

    public PresetVersionEntity() {}

    public PresetVersionEntity(@Nonnull UUID presetKey,
                               @Nonnull Integer version,
                               @Nonnull GroupLayout rootElement,
                               @Nonnull FormStatus status,
                               @Nullable LocalDateTime created,
                               @Nullable LocalDateTime updated,
                               @Nullable LocalDateTime published,
                               @Nullable LocalDateTime revoked) {
        this.presetKey = presetKey;
        this.version = version;
        this.rootElement = rootElement;
        this.status = status;
        this.created = created;
        this.updated = updated;
        this.published = published;
        this.revoked = revoked;
    }

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // Equals & HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        PresetVersionEntity that = (PresetVersionEntity) object;
        return presetKey.equals(that.presetKey) && version.equals(that.version) && rootElement.equals(that.rootElement) && status == that.status && Objects.equals(created, that.created) && Objects.equals(updated, that.updated) && Objects.equals(published, that.published) && Objects.equals(revoked, that.revoked);
    }

    @Override
    public int hashCode() {
        int result = presetKey.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + rootElement.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + Objects.hashCode(created);
        result = 31 * result + Objects.hashCode(updated);
        result = 31 * result + Objects.hashCode(published);
        result = 31 * result + Objects.hashCode(revoked);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public UUID getPresetKey() {
        return presetKey;
    }

    public PresetVersionEntity setPresetKey(@Nonnull UUID presetKey) {
        this.presetKey = presetKey;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public PresetVersionEntity setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }

    @Nonnull
    public GroupLayout getRootElement() {
        return rootElement;
    }

    public PresetVersionEntity setRootElement(@Nonnull GroupLayout rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    @Nonnull
    public FormStatus getStatus() {
        return status;
    }

    public PresetVersionEntity setStatus(@Nonnull FormStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return created;
    }

    public PresetVersionEntity setCreated(@Nullable LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdated() {
        return updated;
    }

    public PresetVersionEntity setUpdated(@Nullable LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getPublished() {
        return published;
    }

    public PresetVersionEntity setPublished(@Nullable LocalDateTime published) {
        this.published = published;
        return this;
    }

    @Nullable
    public LocalDateTime getRevoked() {
        return revoked;
    }

    public PresetVersionEntity setRevoked(@Nullable LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    // endregion
}
