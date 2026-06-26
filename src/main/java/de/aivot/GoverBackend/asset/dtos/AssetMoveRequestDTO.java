package de.aivot.GoverBackend.asset.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;

public record AssetMoveRequestDTO(
        @Nonnull
        @NotBlank(message = "Das Feld sourcePath ist ein Pflichtfeld.")
        String sourcePath,
        @Nonnull
        @NotBlank(message = "Das Feld targetPath ist ein Pflichtfeld.")
        String targetPath
) {
}

