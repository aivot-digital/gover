package de.aivot.prosuna.backend.asset.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;

public record AssetMoveRequestDTO(
        @Nonnull
        @NotBlank(message = "Geben Sie den Quellpfad an.")
        String sourcePath,
        @Nonnull
        @NotBlank(message = "Geben Sie den Zielpfad an.")
        String targetPath
) {
}

