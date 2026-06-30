package de.aivot.gover.backend.config.dtos;

import de.aivot.gover.backend.config.entities.UserConfigEntity;
import de.aivot.gover.backend.config.models.UserConfigDefinition;
import de.aivot.gover.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record UserConfigRequestDto(
        @Nullable
        Object value
) {
    @Nonnull
    public UserConfigEntity toEntity(UserConfigDefinition userConfigDefinition) throws ResponseException {
        var entity = new UserConfigEntity();
        entity.setValue(userConfigDefinition.serializeValueToDB(value));
        return entity;
    }
}