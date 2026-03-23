package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public record IdentityProviderDetailsDTO(
        @Nonnull UUID key,
        @Nonnull String metadataIdentifier,
        @Nonnull IdentityProviderType type,
        @Nullable String pkceMethod,
        @Nonnull String name,
        @Nonnull String description,
        @Nullable UUID iconAssetKey,
        @Nonnull String authorizationEndpoint,
        @Nonnull String tokenEndpoint,
        @Nullable String userinfoEndpoint,
        @Nullable String endSessionEndpoint,
        @Nonnull String clientId,
        @Nullable UUID clientSecretKey,
        @Nonnull List<IdentityAttributeMapping> attributes,
        @Nonnull List<String> defaultScopes,
        @Nonnull List<IdentityAdditionalParameter> additionalParams,
        @Nonnull Boolean isEnabled,
        @Nonnull Boolean isTestProvider
) {
    public static IdentityProviderDetailsDTO from(IdentityProviderEntity entity) {
        return new IdentityProviderDetailsDTO(
                entity.getKey(),
                entity.getMetadataIdentifier(),
                entity.getType(),
                entity.getPkceMethod(),
                entity.getName(),
                entity.getDescription(),
                entity.getIconAssetKey(),
                entity.getAuthorizationEndpoint(),
                entity.getTokenEndpoint(),
                entity.getUserinfoEndpoint(),
                entity.getEndSessionEndpoint(),
                entity.getClientId(),
                entity.getClientSecretKey(),
                entity.getAttributes(),
                entity.getDefaultScopes(),
                entity.getAdditionalParams(),
                entity.getIsEnabled(),
                entity.getIsTestProvider()
        );
    }
}
