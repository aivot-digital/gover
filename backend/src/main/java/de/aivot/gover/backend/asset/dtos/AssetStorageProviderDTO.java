package de.aivot.gover.backend.asset.dtos;

import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.models.StorageProviderMetadataAttribute;
import jakarta.annotation.Nonnull;

import java.util.List;

// Asset users need provider metadata for navigation and uploads, but must not receive provider configuration.
public record AssetStorageProviderDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String name,
        @Nonnull
        Boolean readOnlyStorage,
        @Nonnull
        Long maxFileSizeInBytes,
        @Nonnull
        List<StorageProviderMetadataAttribute> metadataAttributes
) {
    @Nonnull
    public static AssetStorageProviderDTO fromEntity(@Nonnull StorageProviderEntity entity) {
        return new AssetStorageProviderDTO(
                entity.getId(),
                entity.getName(),
                entity.getReadOnlyStorage(),
                entity.getMaxFileSizeInBytes(),
                entity.getMetadataAttributes()
        );
    }
}
