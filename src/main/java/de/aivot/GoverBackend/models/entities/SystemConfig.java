package de.aivot.GoverBackend.models.entities;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
public class SystemConfig {
    @Id
    @Column(length = 64)
    private String key;

    @NotNull
    @Column(length = 96)
    private String value;

    @NotNull
    @ColumnDefault("FALSE")
    private boolean publicConfig;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isPublicConfig() {
        return publicConfig;
    }

    public void setPublicConfig(boolean publicConfig) {
        this.publicConfig = publicConfig;
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


    // endregion
}
