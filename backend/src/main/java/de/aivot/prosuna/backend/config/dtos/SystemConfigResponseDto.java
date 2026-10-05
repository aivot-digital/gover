package de.aivot.prosuna.backend.config.dtos;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
