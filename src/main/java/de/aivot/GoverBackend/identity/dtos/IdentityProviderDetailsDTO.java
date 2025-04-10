package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public record IdentityProviderDetailsDTO(
        @Nonnull String key,
        @Nonnull String metadataIdentifier,
        @Nonnull IdentityProviderType type,
        @Nonnull String name,
        @Nonnull String description,
        @Nullable String iconAssetKey,
        @Nonnull String authorizationEndpoint,
        @Nonnull String tokenEndpoint,
        @Nonnull String userinfoEndpoint,
        @Nonnull String endSessionEndpoint,
        @Nonnull String clientId,
        @Nullable String clientSecretKey,
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
