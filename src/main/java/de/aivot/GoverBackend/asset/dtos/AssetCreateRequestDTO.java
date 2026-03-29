package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record AssetCreateRequestDTO(
        @Nullable
        @NotNull(message = "Das Feld isPrivate ist ein Pflichtfeld")
        Boolean isPrivate,

        @Nullable
        @NotNull(message = "Die Metadaten sind ein Pflichtfeld")
        StorageItemMetadata metadata
) {
}
