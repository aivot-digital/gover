package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;

import javax.annotation.Nonnull;
import java.util.List;

public record IdentityProviderListDTO(
        @Nonnull String key,
        @Nonnull String metadataIdentifier,
        @Nonnull IdentityProviderType type,
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
                entity.getName(),
                entity.getDescription(),
                entity.getAttributes(),
                entity.getIsEnabled(),
                entity.getIsTestProvider()
        );
    }
}
