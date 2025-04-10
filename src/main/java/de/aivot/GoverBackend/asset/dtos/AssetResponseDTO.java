package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.asset.entities.AssetEntity;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

public record AssetResponseDTO(
        @Nonnull
        String key,
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
