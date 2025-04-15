package de.aivot.GoverBackend.identity.models;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;

import javax.annotation.Nonnull;
import java.util.Map;

public record IdentityData(
        @Nonnull
        String providerKey,
        @Nonnull
        Map<String, String> attributes
) {
    public static IdentityData from(IdentityCacheEntity entity) {
        return new IdentityData(
                entity.getProviderKey(),
                entity.getIdentityData()
        );
    }
}
