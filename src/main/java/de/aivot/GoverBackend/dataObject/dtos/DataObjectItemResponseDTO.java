package de.aivot.GoverBackend.dataObject.dtos;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;

public record DataObjectItemResponseDTO(
        @Nonnull
        String schemaKey,
        @Nonnull
        String id,
        @Nonnull
        AuthoredElementValues data,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        LocalDateTime updated
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
