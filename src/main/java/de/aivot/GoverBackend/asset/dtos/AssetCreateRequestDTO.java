package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record AssetCreateRequestDTO(
        @Nonnull
        @NotBlank(message = "storagePathFromRoot is required")
        @Length(min = 2, max = 2048, message = "storagePathFromRoot must be between 2 and 2048 characters")
        String storagePathFromRoot,

        @Nonnull
        @NotNull(message = "Is private is required")
        Boolean isPrivate,

        @Nullable
        StorageItemMetadata metadata
) implements RequestDTO<AssetEntity> {

    @Override
    public AssetEntity toEntity() {
        var asset = new AssetEntity();
        asset.setStoragePathFromRoot(storagePathFromRoot());
        asset.setPrivate(isPrivate());
        return asset;
    }
}
