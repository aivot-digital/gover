package de.aivot.prosuna.backend.config.dtos;

import de.aivot.prosuna.backend.config.entities.UserConfigEntity;
import de.aivot.prosuna.backend.config.models.UserConfigDefinition;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record UserConfigResponseDto(
        @Nonnull String userId,
        @Nonnull String key,
        @Nullable Object value
) {
    @Nonnull
    public static UserConfigResponseDto fromEntity(
            @Nonnull UserConfigEntity userConfigEntity,
            @Nonnull UserConfigDefinition userConfigDefinition
    ) throws ResponseException {
        var value = userConfigDefinition
                .parseValueFromDB(userConfigEntity.getValue());

        return new UserConfigResponseDto(
                userConfigEntity.getUserId(),
                userConfigEntity.getKey(),
                value
        );
    }
}
