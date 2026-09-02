package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;

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
