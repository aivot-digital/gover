package de.aivot.GoverBackend.config.dtos;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record SystemConfigRequestDto(
        @Nullable
        Object value
) {
    @Nonnull
    public SystemConfigEntity toEntity(@Nonnull SystemConfigDefinition definition) throws ResponseException {
        var entity = new SystemConfigEntity();
        entity.setValue(definition.serializeValueToDB(value));
        return entity;
    }
}
