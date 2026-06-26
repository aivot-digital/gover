package de.aivot.gover.backend.dataObject.dtos;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.gover.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;

import jakarta.annotation.Nonnull;
import java.time.Instant;

public record DataObjectItemResponseDTO(
        @Nonnull
        String schemaKey,
        @Nonnull
        String id,
        @Nonnull
        AuthoredElementValues data,
        @Nonnull
        Instant created,
        @Nonnull
        Instant updated
) {
    public static DataObjectItemResponseDTO fromEntity(DataObjectItemEntity entity, DataObjectSchemaEntity schema) {
        var elementData = ObjectMapperFactory
                .getInstance()
                .convertValue(entity.getData(), AuthoredElementValues.class);

        return new DataObjectItemResponseDTO(
                entity.getSchemaKey(),
                entity.getId(),
                elementData,
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
