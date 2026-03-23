package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public record IdentityProviderListDTO(
        @Nonnull UUID key,
        @Nonnull String metadataIdentifier,
        @Nonnull IdentityProviderType type,
        @Nullable String pkceMethod,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull List<IdentityAttributeMapping> attributes,
        @Nonnull Boolean isEnabled,
        @Nonnull Boolean isTestProvider
) {
    public static IdentityProviderListDTO from(IdentityProviderEntity entity) {
        return new IdentityProviderListDTO(
                entity.getKey(),
                entity.getMetadataIdentifier(),
                entity.getType(),
                entity.getPkceMethod(),
                entity.getName(),
                entity.getDescription(),
                entity.getAttributes(),
                entity.getIsEnabled(),
                entity.getIsTestProvider()
        );
    }
}
