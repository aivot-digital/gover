package de.aivot.GoverBackend.asset.entities;

import de.aivot.GoverBackend.storage.converters.StorageItemMetadataConverter;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets_with_metadata")
public class AssetWithMetadataEntity {
    @Id
    @Nonnull
    private UUID key;

    @Nonnull
    private String filename;

    @Nullable
    private String uploaderId;

    @Nullable
    private String contentType;

    @Nonnull
    private Boolean isPrivate;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private Integer storageProviderId;

    @Nonnull
    private String storagePathFromRoot;

    @Nonnull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = StorageItemMetadataConverter.class)
    private StorageItemMetadata metadata;

    @Nonnull
    public UUID getKey() {
        return key;
    }

    @Nonnull
    public String getFilename() {
        return filename;
    }

    @Nullable
    public String getUploaderId() {
        return uploaderId;
    }

    @Nullable
    public String getContentType() {
        return contentType;
    }

    @Nonnull
    public Boolean getPrivate() {
        return isPrivate;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    @Nonnull
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    @Nonnull
    public String getStoragePathFromRoot() {
        return storagePathFromRoot;
    }

    @Nonnull
    public StorageItemMetadata getMetadata() {
        return metadata;
    }
}
