package de.aivot.GoverBackend.config.dtos;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;

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