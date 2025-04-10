package de.aivot.GoverBackend.identity.dtos;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public record IdentityProviderRequestDTO(
        @Nonnull
        @NotNull(message = "Der Identifikator für die Metadaten des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 64, message = "Der Identifikator für die Metadaten des Nutzerkontenanbieters muss zwischen 1 und 64 Zeichen lang sein.")
        String metadataIdentifier,
        @Nonnull
        @NotNull(message = "Der Name des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 64, message = "Der Name des Nutzerkontenanbieters muss zwischen 1 und 64 Zeichen lang sein.")
        String name,
        @Nonnull
        @NotNull(message = "Die Beschreibung des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Die Beschreibung des Nutzerkontenanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String description,
        @Nullable
        String iconAssetKey,
        @Nonnull
        @NotNull(message = "Der Autorisierungsendpunkt des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Der Autorisierungsendpunkt des Nutzerkontenanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String authorizationEndpoint,
        @Nonnull
        @NotNull(message = "Der Tokenendpunkt des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Der Tokenendpunkt des Nutzerkontenanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String tokenEndpoint,
        @Nonnull
        @NotNull(message = "Der Benutzerinfoendpunkt des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Der Benutzerinfoendpunkt des Nutzerkontenanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String userinfoEndpoint,
        @Nonnull
        @Size(min = 1, max = 255, message = "Der End-Session-Endpunkt des Nutzerkontenanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        @NotNull(message = "Der End-Session-Endpunkt des Nutzerkontenanbieters ist erforderlich.")
        String endSessionEndpoint,
        @Nonnull
        @NotNull(message = "Die Client-ID des Nutzerkontenanbieters ist erforderlich.")
        @Size(min = 1, max = 32, message = "Die Client-ID des Nutzerkontenanbieters muss zwischen 1 und 32 Zeichen lang sein.")
        String clientId,
        @Nullable
        String clientSecretKey,
        @Nonnull
        @NotNull(message = "Die Attribute des Nutzerkontenanbieters sind erforderlich.")
        List<IdentityAttributeMapping> attributes,
        @Nonnull
        @NotNull(message = "Die Standard-Scopes des Nutzerkontenanbieters sind erforderlich.")
        List<String> defaultScopes,
        @Nonnull
        @NotNull(message = "Die zusätzlichen Parameter des Nutzerkontenanbieters sind erforderlich.")
        List<IdentityAdditionalParameter> additionalParams,
        @Nonnull
        @NotNull(message = "Der Status des Nutzerkontenanbieters ist erforderlich.")
        Boolean isEnabled,
        @Nonnull
        @NotNull(message = "Der Teststatus des Nutzerkontenanbieters ist erforderlich.")
        Boolean isTestProvider
) {
    @Nonnull
    public IdentityProviderEntity toEntity() {
        return new IdentityProviderEntity()
                .setKey("")
                .setType(IdentityProviderType.Custom)
                .setName(name)
                .setDescription(description)
                .setIconAssetKey(iconAssetKey)
                .setMetadataIdentifier(metadataIdentifier)
                .setAuthorizationEndpoint(authorizationEndpoint)
                .setTokenEndpoint(tokenEndpoint)
                .setUserinfoEndpoint(userinfoEndpoint)
                .setEndSessionEndpoint(endSessionEndpoint)
                .setClientId(clientId)
                .setClientSecretKey(clientSecretKey)
                .setAttributes(attributes)
                .setDefaultScopes(defaultScopes)
                .setAdditionalParams(additionalParams)
                .setIsEnabled(isEnabled)
                .setIsTestProvider(isTestProvider);
    }
}
