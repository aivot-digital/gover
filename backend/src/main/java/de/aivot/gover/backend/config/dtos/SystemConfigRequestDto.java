package de.aivot.gover.backend.config.dtos;

import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record SystemConfigRequestDto(
        @Nullable
        Object value,
        @Nullable
        Boolean changeConfirmed
) {
    @Nonnull
    public SystemConfigEntity toEntity(@Nonnull SystemConfigDefinition definition) throws ResponseException {
        var entity = new SystemConfigEntity();
        entity.setValue(definition.serializeValueToDB(value));
        return entity;
    }
}
