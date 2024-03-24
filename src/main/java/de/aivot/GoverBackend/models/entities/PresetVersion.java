package de.aivot.GoverBackend.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.converters.GroupLayoutConverter;
import de.aivot.GoverBackend.converters.JacksonGroupLayoutDeserializer;
import de.aivot.GoverBackend.converters.JacksonGroupLayoutSerializer;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.lib.PresetVersionKey;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@IdClass(PresetVersionKey.class)
@Table(name = "preset_versions")
public class PresetVersion {
    @Id
    @Column(columnDefinition = "uuid")
    private String preset;

    @Id
    private String version;

    @NotNull
    @Convert(converter = GroupLayoutConverter.class)
    @JsonSerialize(converter = JacksonGroupLayoutSerializer.class)
    @JsonDeserialize(converter = JacksonGroupLayoutDeserializer.class)
    @Column(columnDefinition = "jsonb")
    private GroupLayout root;

    private LocalDateTime publishedAt;

    private LocalDateTime publishedStoreAt;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // region Getters & Setters

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public GroupLayout getRoot() {
        return root;
    }

    public void setRoot(GroupLayout root) {
        this.root = root;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getPublishedStoreAt() {
        return publishedStoreAt;
    }

    public void setPublishedStoreAt(LocalDateTime publishedStoreAt) {
        this.publishedStoreAt = publishedStoreAt;
    }


    // endregion


}
