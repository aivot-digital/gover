package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;

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
    public static DataObjectItemResponseDTO fromEntity(
            @Nonnull DataObjectItemEntity entity,
            @Nonnull DataObjectSchemaEntity schema,
            @Nonnull AuthoredInputValueService authoredInputValueService
    ) {
        var elementData = authoredInputValueService.toLiteralAuthoredElementValues(
                schema.getSchema(),
                entity.getData()
        );

        return new DataObjectItemResponseDTO(
                entity.getSchemaKey(),
                entity.getId(),
                elementData,
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
