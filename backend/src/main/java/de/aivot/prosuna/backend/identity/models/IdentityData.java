package de.aivot.prosuna.backend.identity.models;

import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public record IdentityData(
        @Nonnull
        String sessionId,
        @Nonnull
        String identityId,
        @Nonnull
        IdentityType type,
        @Nullable
        UUID providerKey,
        @Nullable
        String metadataIdentifier,
        @Nullable
        String emailAddress,
        @Nonnull
        Map<String, String> attributes,
        @Nullable
        Integer communicationProviderBindingId,
        @Nonnull
        Map<String, Object> communicationProviderData
) implements Serializable {
    public IdentityData {
        attributes = attributes == null ? Map.of() : attributes;
        communicationProviderData = communicationProviderData == null ? Map.of() : communicationProviderData;

        if (type == IdentityType.Email) {
            if (emailAddress == null || emailAddress.isBlank()) {
                throw new IllegalArgumentException("Eine E-Mail-Identität benötigt eine E-Mail-Adresse.");
            }
            if (providerKey != null || metadataIdentifier != null || communicationProviderBindingId != null) {
                throw new IllegalArgumentException("Eine E-Mail-Identität darf keine Anbieterdaten enthalten.");
            }
            emailAddress = emailAddress.trim();
            attributes = Map.of("email", emailAddress);
            communicationProviderData = Map.of();
        } else {
            if (providerKey == null || metadataIdentifier == null || metadataIdentifier.isBlank()) {
                throw new IllegalArgumentException("Eine Anbieteridentität benötigt Anbieter- und Metadaten.");
            }
            if (emailAddress != null) {
                throw new IllegalArgumentException("Eine Anbieteridentität darf keine direkte E-Mail-Adresse enthalten.");
            }
        }
    }

    public static IdentityData from(@Nonnull IdentityCacheEntity entity) {
        return new IdentityData(
                entity.getSessionId(),
                entity.getIdentityId(),
                entity.getType(),
                entity.getProviderKey(),
                entity.getMetadataIdentifier(),
                entity.getEmailAddress(),
                entity.getIdentityData() != null ? entity.getIdentityData() : Map.of(),
                entity.getCommunicationProviderBindingId(),
                entity.getCommunicationProviderData() != null ? entity.getCommunicationProviderData() : Map.of()
        );
    }
}
