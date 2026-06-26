package de.aivot.GoverBackend.dataObject.dtos;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;

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
