package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import jakarta.annotation.Nonnull;

import java.util.List;

public record AssetFolderGroupResponseDTO(
        @Nonnull Integer storageProviderId,
        @Nonnull String storageProviderName,
        @Nonnull List<StorageIndexItemEntity> folders,
        @Nonnull List<AssetResponseDTO> files
) {
}
