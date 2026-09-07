package de.aivot.prosuna.backend.identity.models;

import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the data of an identity, which can be either an email identity or a provider identity.
 * This record is used to encapsulate all relevant information about an identity, including its type,
 * associated provider details, email address (if applicable), and any additional attributes or communication data.
 *
 * @param sessionId                      The session ID associated with the identity, used identifying an identity after a successful authentication and before the identity is stored in the database.
 * @param identityId                     The unique identifier by which the identity is known in the process instance. This id is not unique in the system, but only in the context of a process instance. It is used to identify the identity in the process instance and to retrieve the identity data. This is NOT the id by which the identity is unique in the originating identity provider system.
 * @param type                           The type of the identity, which can be either an email identity or a provider identity.
 * @param providerKey                    The unique identifier of the identity provider this identity originates from, if the identity is a provider identity. This is null for email identities.
 * @param metadataIdentifier             The identifier by which ui elements can reference attributes of this identity, e.g. __meta__.first_name. This is null for email identities.
 * @param emailAddress                   The email address of the identity, if the identity is an email identity. This is null for provider identities.
 * @param attributes                     A map of attributes associated with the identity, provides by the identity provider after a successful authentication. For email identities, this map contains only the email address under the key "email".
 * @param communicationProviderBindingId The unique identifier of the communication provider binding associated with this identity, if applicable. This is null for email identities.
 * @param communicationProviderData      A map of data associated with the communication provider for this identity, if applicable. This is null for email identities.
 */
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
