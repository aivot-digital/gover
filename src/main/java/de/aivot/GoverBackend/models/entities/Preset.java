package de.aivot.GoverBackend.models.entities;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "presets")
public class Preset {
    @Id
    @Column(columnDefinition = "uuid")
    private String key;

    @NotNull
    @Column(length = 255)
    @Length(min = 3, max = 255)
    private String title;

    @Column(columnDefinition = "uuid")
    private String storeId;

    private String currentVersion;

    private String currentStoreVersion;

    private String currentPublishedVersion;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    // region Getters & Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCurrentStoreVersion() {
        return currentStoreVersion;
    }

    public void setCurrentStoreVersion(String currentStoreVersion) {
        this.currentStoreVersion = currentStoreVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getCurrentPublishedVersion() {
        return currentPublishedVersion;
    }

    public void setCurrentPublishedVersion(String currentPublishedVersion) {
        this.currentPublishedVersion = currentPublishedVersion;
    }

    // endregion
}
