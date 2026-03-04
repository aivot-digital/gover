package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.entities.AssetWithMetadataEntity;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssetResponseDTO(
        @Nonnull
        UUID key,
        @Nonnull
        Integer storageProviderId,
        @Nonnull
        String storagePathFromRoot,
        @Nonnull
        String filename,
        @Nonnull
        String contentType,
        @Nullable
        String uploaderId,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        Boolean isPrivate,
        @Nonnull
        StorageItemMetadata metadata
) {
    public static AssetResponseDTO fromEntity(AssetEntity entity) {
        return new AssetResponseDTO(
                entity.getKey(),
                entity.getStorageProviderId(),
                entity.getStoragePathFromRoot(),
                entity.getFilename() != null ? entity.getFilename() : "",
                entity.getContentType() != null ? entity.getContentType() : "application/octet-stream",
                entity.getUploaderId(),
                entity.getCreated(),
                entity.getPrivate(),
                StorageItemMetadata.empty()
        );
    }

    public static AssetResponseDTO fromViewEntity(AssetWithMetadataEntity entity) {
        return new AssetResponseDTO(
                entity.getKey(),
                entity.getStorageProviderId(),
                entity.getStoragePathFromRoot(),
                entity.getFilename(),
                entity.getContentType() != null ? entity.getContentType() : "application/octet-stream",
                entity.getUploaderId(),
                entity.getCreated(),
                entity.getPrivate(),
                entity.getMetadata() != null ? entity.getMetadata() : StorageItemMetadata.empty()
        );
    }
}
