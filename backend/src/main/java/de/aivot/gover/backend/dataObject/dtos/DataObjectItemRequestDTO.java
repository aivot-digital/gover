package de.aivot.gover.backend.dataObject.dtos;

import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.gover.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;

import jakarta.annotation.Nonnull;

public record DataObjectItemRequestDTO(
        @Nonnull
        String id,
        @Nonnull
        AuthoredElementValues data
) {
    public DataObjectItemEntity toEntity(DataObjectSchemaEntity schema) {
        return new DataObjectItemEntity()
                .setSchemaKey(schema.getKey())
                .setId(id)
                .setData(data);
    }
}
