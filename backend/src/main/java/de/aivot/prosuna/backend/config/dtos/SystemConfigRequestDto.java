package de.aivot.prosuna.backend.config.dtos;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record SystemConfigRequestDto(
        @Nonnull
        @NotNull(message = "Der Wert darf nicht null sein.")
        String value,
        @Nullable
        Boolean changeConfirmed
) {
    @Nonnull
    public SystemConfigEntity toEntity() {
        var entity = new SystemConfigEntity();
        // Requests carry the persisted string format; SystemConfigService parses and validates it via the definition.
        entity.setValue(value);
        return entity;
    }
}
