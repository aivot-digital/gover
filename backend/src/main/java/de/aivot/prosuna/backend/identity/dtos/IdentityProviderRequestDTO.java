package de.aivot.prosuna.backend.identity.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityAdditionalParameter;
import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IdentityProviderRequestDTO(
        @Nonnull
        @NotNull(message = "Der Identifikator für die Metadaten des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 64, message = "Der Identifikator für die Metadaten des Identitätsanbieters muss zwischen 1 und 64 Zeichen lang sein.")
        String metadataIdentifier,
        @Nonnull
        @NotBlank(message = "Die Identitätenkennung des Identitätsanbieters ist erforderlich.")
        @Size(max = 255, message = "Die Identitätenkennung des Identitätsanbieters darf maximal 255 Zeichen lang sein.")
        String uniqueIdAttribute,
        @Nonnull
        @NotNull(message = "Der Name des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 64, message = "Der Name des Identitätsanbieters muss zwischen 1 und 64 Zeichen lang sein.")
        String name,
        @Nonnull
        @NotNull(message = "Die Beschreibung des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Die Beschreibung des Identitätsanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String description,
        @Nullable
        UUID iconAssetKey,
        @Nonnull
        @NotNull(message = "Der Autorisierungsendpunkt des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Der Autorisierungsendpunkt des Identitätsanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String authorizationEndpoint,
        @Nonnull
        @NotNull(message = "Der Tokenendpunkt des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 255, message = "Der Tokenendpunkt des Identitätsanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String tokenEndpoint,
        @Nullable
        @Size(min = 1, max = 255, message = "Der Benutzerinfoendpunkt des Identitätsanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String userinfoEndpoint,
        @Nullable
        @Size(min = 1, max = 255, message = "Der End-Session-Endpunkt des Identitätsanbieters muss zwischen 1 und 255 Zeichen lang sein.")
        String endSessionEndpoint,
        @Nonnull
        @NotNull(message = "Die Client-ID des Identitätsanbieters ist erforderlich.")
        @Size(min = 1, max = 128, message = "Die Client-ID des Identitätsanbieters muss zwischen 1 und 32 Zeichen lang sein.")
        String clientId,
        @Nullable
        UUID clientSecretKey,
        @Nonnull
        @NotNull(message = "Die Attribute des Identitätsanbieters sind erforderlich.")
        List<IdentityAttributeMapping> attributes,
        @Nonnull
        @NotNull(message = "Die Standard-Scopes des Identitätsanbieters sind erforderlich.")
        List<String> defaultScopes,
        @Nonnull
        @NotNull(message = "Die zusätzlichen Parameter des Identitätsanbieters sind erforderlich.")
        List<IdentityAdditionalParameter> additionalParams,
        @Nonnull
        @NotNull(message = "Der Status des Identitätsanbieters ist erforderlich.")
        Boolean isEnabled,
        @Nonnull
        @NotNull(message = "Der Teststatus des Identitätsanbieters ist erforderlich.")
        Boolean isTestProvider,
        @Nullable
        String pkceMethod
) {
    @Nonnull
    public IdentityProviderEntity toEntity() {
        return new IdentityProviderEntity()
                .setKey(null)
                .setType(IdentityProviderType.Custom)
                .setName(name)
                .setDescription(description)
                .setIconAssetKey(iconAssetKey)
                .setMetadataIdentifier(metadataIdentifier)
                .setUniqueIdAttribute(uniqueIdAttribute)
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
                .setIsTestProvider(isTestProvider)
                .setPkceMethod(pkceMethod);
    }

    @JsonIgnore
    @AssertTrue(message = "Das Attribut für die eindeutige ID muss in den Attributszuweisungen enthalten sein.")
    public boolean isUniqueIdAttributeMapped() {
        if (uniqueIdAttribute == null || uniqueIdAttribute.isBlank() || attributes == null) {
            return true;
        }

        return attributes.stream()
                .filter(Objects::nonNull)
                .anyMatch(attribute -> Objects.equals(attribute.getKeyInData(), uniqueIdAttribute));
    }
}
