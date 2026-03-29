package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record AssetRequestDTO(
        @Nonnull
        @NotNull(message = "Is private is required")
        Boolean isPrivate,

        @Nullable
        StorageItemMetadata metadata
) {
}
