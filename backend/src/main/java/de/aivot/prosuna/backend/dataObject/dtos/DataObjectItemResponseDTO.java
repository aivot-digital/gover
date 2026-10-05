package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

public record DataObjectItemResponseDTO(
        @Nonnull
        String schemaKey,
        @Nonnull
        String id,
        @Nonnull
        Map<String, Object> data,
        @Nonnull
        Instant created,
        @Nonnull
        Instant updated
) {
    @Nonnull
    public static DataObjectItemResponseDTO fromEntity(
            @Nonnull DataObjectItemEntity entity
    ) {
        return new DataObjectItemResponseDTO(
                entity.getSchemaKey(),
                entity.getId(),
                entity.getData(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
