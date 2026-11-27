package de.aivot.prosuna.backend.asset.dtos;

import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record AssetCreateRequestDTO(
        @Nullable
        @NotNull(message = "Geben Sie an, ob die Datei geschützt ist.")
        Boolean isPrivate,

        @Nullable
        @NotNull(message = "Die Metadaten müssen angegeben werden.")
        StorageItemMetadata metadata
) {
}
