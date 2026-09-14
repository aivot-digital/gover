package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DataObjectItemRequestDTO(
        @Nonnull
        String id,
        @Nonnull
        @NotNull
        Map<String, Object> data
) {
    @Nonnull
    public DataObjectItemEntity toEntity(@Nonnull DataObjectSchemaEntity schema) {
        return new DataObjectItemEntity()
                .setSchemaKey(schema.getKey())
                .setId(id)
                .setData(data);
    }
}
