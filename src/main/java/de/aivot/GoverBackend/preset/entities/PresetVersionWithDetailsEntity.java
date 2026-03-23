package de.aivot.GoverBackend.preset.entities;

import de.aivot.GoverBackend.core.converters.GroupLayoutConverter;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(PresetVersionWithDetailsEntityId.class)
@Table(name = "preset_version_with_details")
public class PresetVersionWithDetailsEntity {
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
    private FormStatus status;

    @Nullable
    private LocalDateTime created;

    @Nullable
    private LocalDateTime updated;

    @Nullable
    private LocalDateTime published;

    @Nullable
    private LocalDateTime revoked;

    // Equals & HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        PresetVersionWithDetailsEntity that = (PresetVersionWithDetailsEntity) object;
        return key.equals(that.key) && title.equals(that.title) && Objects.equals(publishedVersion, that.publishedVersion) && Objects.equals(draftedVersion, that.draftedVersion) && versionCount.equals(that.versionCount) && presetKey.equals(that.presetKey) && version.equals(that.version) && rootElement.equals(that.rootElement) && status == that.status && Objects.equals(created, that.created) && Objects.equals(updated, that.updated) && Objects.equals(published, that.published) && Objects.equals(revoked, that.revoked);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + Objects.hashCode(publishedVersion);
        result = 31 * result + Objects.hashCode(draftedVersion);
        result = 31 * result + versionCount.hashCode();
        result = 31 * result + presetKey.hashCode();
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
    public UUID getKey() {
        return key;
    }

    public PresetVersionWithDetailsEntity setKey(@Nonnull UUID key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    public PresetVersionWithDetailsEntity setTitle(@Nonnull String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public PresetVersionWithDetailsEntity setPublishedVersion(@Nullable Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    @Nullable
    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public PresetVersionWithDetailsEntity setDraftedVersion(@Nullable Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    @Nonnull
    public UUID getPresetKey() {
        return presetKey;
    }

    public PresetVersionWithDetailsEntity setPresetKey(@Nonnull UUID presetKey) {
        this.presetKey = presetKey;
        return this;
    }

    @Nonnull
    public Integer getVersion() {
        return version;
    }

    public PresetVersionWithDetailsEntity setVersion(@Nonnull Integer version) {
        this.version = version;
        return this;
    }

    @Nonnull
    public GroupLayoutElement getRootElement() {
        return rootElement;
    }

    public PresetVersionWithDetailsEntity setRootElement(@Nonnull GroupLayoutElement rootElement) {
        this.rootElement = rootElement;
        return this;
    }

    @Nonnull
    public FormStatus getStatus() {
        return status;
    }

    public PresetVersionWithDetailsEntity setStatus(@Nonnull FormStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return created;
    }

    public PresetVersionWithDetailsEntity setCreated(@Nullable LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nullable
    public LocalDateTime getUpdated() {
        return updated;
    }

    public PresetVersionWithDetailsEntity setUpdated(@Nullable LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getPublished() {
        return published;
    }

    public PresetVersionWithDetailsEntity setPublished(@Nullable LocalDateTime published) {
        this.published = published;
        return this;
    }

    @Nullable
    public LocalDateTime getRevoked() {
        return revoked;
    }

    public PresetVersionWithDetailsEntity setRevoked(@Nullable LocalDateTime revoked) {
        this.revoked = revoked;
        return this;
    }

    @Nonnull
    public Integer getVersionCount() {
        return versionCount;
    }

    public PresetVersionWithDetailsEntity setVersionCount(@Nonnull Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }


    // endregion
}
