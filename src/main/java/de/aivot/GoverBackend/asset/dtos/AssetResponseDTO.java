package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.asset.entities.AssetEntity;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssetResponseDTO(
        @Nonnull
        UUID key,
        @Nonnull
        String filename,
        @Nonnull
        String contentType,
        @Nonnull
        String uploaderId,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        Boolean isPrivate
) {
    public static AssetResponseDTO fromEntity(AssetEntity entity) {
        return new AssetResponseDTO(
                entity.getKey(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getUploaderId(),
                entity.getCreated(),
                entity.getPrivate()
        );
    }
}
