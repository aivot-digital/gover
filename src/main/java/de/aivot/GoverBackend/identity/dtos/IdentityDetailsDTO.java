package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public record IdentityDetailsDTO(
        @Nonnull
        IdentityProviderType type,
        @Nonnull
        UUID key,
        @Nonnull
        String name,
        @Nullable
        UUID iconAssetKey,
        @Nonnull
        String metadataIdentifier
) {
    public static IdentityDetailsDTO from(@Nonnull IdentityProviderEntity entity) {
        return new IdentityDetailsDTO(
                entity.getType(),
                entity.getKey(),
                entity.getName(),
                entity.getIconAssetKey(),
                entity.getMetadataIdentifier()
        );
    }
}
