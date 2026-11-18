package de.aivot.prosuna.backend.asset.dtos;

import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record AssetRequestDTO(
        @Nonnull
        @NotNull(message = "Geben Sie an, ob die Datei geschützt ist.")
        Boolean isPrivate,

        @Nullable
        StorageItemMetadata metadata
) {
}
