package de.aivot.GoverBackend.dataObject.dtos;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.elements.models.ElementData;

import jakarta.annotation.Nonnull;

public record DataObjectItemRequestDTO(
        @Nonnull
        String id,
        @Nonnull
        ElementData data
) {
    public DataObjectItemEntity toEntity(DataObjectSchemaEntity schema) {
        var valueMap = ElementData
                .toValueMap(schema.getSchema(), data);

        return new DataObjectItemEntity()
                .setSchemaKey(schema.getKey())
                .setId(id)
                .setData(valueMap);
    }
}
