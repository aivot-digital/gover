package de.aivot.gover.backend.asset.dtos;

import de.aivot.gover.backend.storage.models.StorageItemMetadata;
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
