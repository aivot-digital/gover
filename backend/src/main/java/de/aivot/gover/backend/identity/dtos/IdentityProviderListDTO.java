package de.aivot.gover.backend.identity.dtos;

import de.aivot.gover.backend.identity.entities.IdentityProviderEntity;
import de.aivot.gover.backend.identity.enums.IdentityProviderType;
import de.aivot.gover.backend.identity.models.IdentityAttributeMapping;

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
        @Nullable UUID iconAssetKey,
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
                entity.getIconAssetKey(),
                entity.getAttributes(),
                entity.getIsEnabled(),
                entity.getIsTestProvider()
        );
    }
}
