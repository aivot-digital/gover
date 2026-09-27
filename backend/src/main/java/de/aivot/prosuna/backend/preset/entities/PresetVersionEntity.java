package de.aivot.prosuna.backend.preset.entities;

import de.aivot.prosuna.backend.core.converters.GroupLayoutConverter;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.preset.enums.PresetStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
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
    private GroupLayoutElement rootElement;

    @Nonnull
    private PresetStatus status;

    @Nullable
    private Instant created;

    @Nullable
    private Instant updated;

    @Nullable
    private Instant published;

    @Nullable
    private Instant revoked;

    public PresetVersionEntity() {}

    public PresetVersionEntity(@Nonnull UUID presetKey,
                               @Nonnull Integer version,
                               @Nonnull GroupLayoutElement rootElement,
                               @Nonnull PresetStatus status,
                               @Nullable Instant created,
                               @Nullable Instant updated,
                               @Nullable Instant published,
                               @Nullable Instant revoked) {
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
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
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
    public GroupLayoutElement getRootElement() {
        return rootElement;
    }

    public PresetVersionEntity setRootElement(@Nonnull GroupLayoutElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    @Nonnull
    public PresetStatus getStatus() {
        return status;
    }

    public PresetVersionEntity setStatus(@Nonnull PresetStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public Instant getCreated() {
        return created;
    }

    public PresetVersionEntity setCreated(@Nullable Instant created) {
        this.created = created;
        return this;
    }

    @Nullable
    public Instant getUpdated() {
        return updated;
    }

    public PresetVersionEntity setUpdated(@Nullable Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Instant getPublished() {
        return published;
    }

    public PresetVersionEntity setPublished(@Nullable Instant published) {
        this.published = published;
        return this;
    }

    @Nullable
    public Instant getRevoked() {
        return revoked;
    }

    public PresetVersionEntity setRevoked(@Nullable Instant revoked) {
        this.revoked = revoked;
        return this;
    }

    // endregion
}
