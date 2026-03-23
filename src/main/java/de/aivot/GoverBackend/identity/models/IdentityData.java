package de.aivot.GoverBackend.identity.models;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public record IdentityData(
        @Nonnull
        UUID identityId,
        @Nonnull
        UUID providerKey,
        @Nonnull
        String metadataIdentifier,
        @Nonnull
        Map<String, String> attributes
) implements Serializable {
    public static IdentityData from(@Nonnull IdentityCacheEntity entity) {
        var uuid = UUID.fromString(entity.getProviderKey());

        return new IdentityData(
                entity.getSessionId(),
                uuid,
                entity.getMetadataIdentifier(),
                entity.getIdentityData() != null ? entity.getIdentityData() : Map.of()
        );
    }
}
