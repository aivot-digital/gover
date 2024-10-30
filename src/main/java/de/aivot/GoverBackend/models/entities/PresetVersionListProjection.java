package de.aivot.GoverBackend.models.entities;

import java.time.LocalDateTime;

public interface PresetVersionListProjection {
    String getPreset();

    String getVersion();

    LocalDateTime getCreated();

    LocalDateTime getUpdated();

    LocalDateTime getPublishedAt();

    LocalDateTime getPublishedStoreAt();
}
