package de.aivot.gover.backend.identity.models;

import de.aivot.gover.backend.identity.cache.entities.IdentityCacheEntity;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public record IdentityData(
        @Nonnull
        String sessionId,
        @Nonnull
        String identityId,
        @Nonnull
        UUID providerKey,
        @Nonnull
        String metadataIdentifier,
        @Nonnull
        Map<String, String> attributes
) implements Serializable {
    public static IdentityData from(@Nonnull IdentityCacheEntity entity) {
        return new IdentityData(
                entity.getSessionId(),
                entity.getIdentityId(),
                entity.getProviderKey(),
                entity.getMetadataIdentifier(),
                entity.getIdentityData() != null ? entity.getIdentityData() : Map.of()
        );
    }
}
