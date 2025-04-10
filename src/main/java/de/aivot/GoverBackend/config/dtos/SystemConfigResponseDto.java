package de.aivot.GoverBackend.config.dtos;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record SystemConfigResponseDto(
        @Nonnull String key,
        @Nullable Object value
) {
    @Nonnull
    public static SystemConfigResponseDto fromEntity(
            @Nonnull SystemConfigEntity systemConfigEntity,
            @Nonnull SystemConfigDefinition systemConfigDefinition
    ) throws ResponseException {
        var value = systemConfigDefinition
                .parseValueFromDB(systemConfigEntity.getValue());

        return new SystemConfigResponseDto(
                systemConfigDefinition.getKey(),
                value
        );
    }
}
