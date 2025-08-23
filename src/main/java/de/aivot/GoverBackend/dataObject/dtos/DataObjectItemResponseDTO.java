package de.aivot.GoverBackend.dataObject.dtos;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.elements.models.ElementData;

import javax.annotation.Nonnull;

public record DataObjectItemResponseDTO(
        @Nonnull
        String schemaKey,
        @Nonnull
        String id,
        @Nonnull
        ElementData data
) {
    public static DataObjectItemResponseDTO fromEntity(DataObjectItemEntity entity, DataObjectSchemaEntity schema) {
        var elementData = ElementData
                .fromValueMap(schema.getSchema(), entity.getData());

        return new DataObjectItemResponseDTO(
                entity.getSchemaKey(),
                entity.getId(),
                elementData
        );
    }
}
