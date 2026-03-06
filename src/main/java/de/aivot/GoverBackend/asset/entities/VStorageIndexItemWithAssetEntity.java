package de.aivot.GoverBackend.asset.entities;

import de.aivot.GoverBackend.storage.converters.StorageItemMetadataConverter;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "v_storage_index_items_with_assets")
@IdClass(VStorageIndexItemWithAssetEntityId.class)
public class VStorageIndexItemWithAssetEntity {
    @Id
    @Nonnull
    private Integer storageProviderId;

    @Id
    @Nonnull
    private String pathFromRoot;

    @Nonnull
    private Boolean directory;

    @Nonnull
    private String filename;

    @Nonnull
    private String mimeType;

    @Nonnull
    private Long sizeInBytes;

    @Nonnull
    private Boolean missing;

    @Nonnull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = StorageItemMetadataConverter.class)
    private StorageItemMetadata metadata;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    @Nullable
    private UUID assetKey;

    @Nullable
    private String assetUploaderId;

    @Nullable
    private Boolean assetIsPrivate;

    // Empty constructor for JPA
    public VStorageIndexItemWithAssetEntity() {
    }

    // Full constructor
    public VStorageIndexItemWithAssetEntity(@Nonnull Integer storageProviderId,
                                            @Nonnull String pathFromRoot,
                                            @Nonnull Boolean directory,
                                            @Nonnull String filename,
                                            @Nonnull String mimeType,
                                            @Nonnull Long sizeInBytes,
                                            @Nonnull Boolean missing,
                                            @Nonnull StorageItemMetadata metadata,
                                            @Nonnull LocalDateTime created,
                                            @Nonnull LocalDateTime updated,
                                            @Nullable UUID assetKey,
                                            @Nullable String assetUploaderId,
                                            @Nullable Boolean assetIsPrivate) {
        this.storageProviderId = storageProviderId;
        this.pathFromRoot = pathFromRoot;
        this.directory = directory;
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeInBytes = sizeInBytes;
        this.missing = missing;
        this.metadata = metadata;
        this.created = created;
        this.updated = updated;
        this.assetKey = assetKey;
        this.assetUploaderId = assetUploaderId;
        this.assetIsPrivate = assetIsPrivate;
    }

    @Nonnull
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public VStorageIndexItemWithAssetEntity setStorageProviderId(@Nonnull Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }

    public VStorageIndexItemWithAssetEntity setPathFromRoot(@Nonnull String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        return this;
    }

    @Nonnull
    public Boolean getDirectory() {
        return directory;
    }

    public VStorageIndexItemWithAssetEntity setDirectory(@Nonnull Boolean directory) {
        this.directory = directory;
        return this;
    }

    @Nonnull
    public String getFilename() {
        return filename;
    }

    public VStorageIndexItemWithAssetEntity setFilename(@Nonnull String filename) {
        this.filename = filename;
        return this;
    }

    @Nonnull
    public String getMimeType() {
        return mimeType;
    }

    public VStorageIndexItemWithAssetEntity setMimeType(@Nonnull String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Nonnull
    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public VStorageIndexItemWithAssetEntity setSizeInBytes(@Nonnull Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }

    @Nonnull
    public Boolean getMissing() {
        return missing;
    }

    public VStorageIndexItemWithAssetEntity setMissing(@Nonnull Boolean missing) {
        this.missing = missing;
        return this;
    }

    @Nonnull
    public StorageItemMetadata getMetadata() {
        return metadata;
    }

    public VStorageIndexItemWithAssetEntity setMetadata(@Nonnull StorageItemMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public VStorageIndexItemWithAssetEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public VStorageIndexItemWithAssetEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public UUID getAssetKey() {
        return assetKey;
    }

    public VStorageIndexItemWithAssetEntity setAssetKey(@Nullable UUID assetKey) {
        this.assetKey = assetKey;
        return this;
    }

    @Nullable
    public String getAssetUploaderId() {
        return assetUploaderId;
    }

    public VStorageIndexItemWithAssetEntity setAssetUploaderId(@Nullable String assetUploaderId) {
        this.assetUploaderId = assetUploaderId;
        return this;
    }

    @Nullable
    public Boolean getAssetIsPrivate() {
        return assetIsPrivate;
    }

    public VStorageIndexItemWithAssetEntity setAssetIsPrivate(@Nullable Boolean assetIsPrivate) {
        this.assetIsPrivate = assetIsPrivate;
        return this;
    }
}
