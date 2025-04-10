package de.aivot.GoverBackend.config.dtos;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
