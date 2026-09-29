package de.aivot.prosuna.backend.identity.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityDataTest {
    @Test
    void storedIdentityWithoutTitleRemainsReadable() {
        var mapper = JsonMapperTestUtils.createMapper();
        var identities = mapper.readValue("""
                {"contact": {
                    "sessionId": "session", "identityId": "contact", "type": "Email",
                    "emailAddress": "person@example.org", "attributes": {}, "communicationProviderData": {}
                }}
                """, IdentityDataMap.class);

        assertNull(identities.get("contact").title());
        assertEquals("person@example.org", identities.get("contact").emailAddress());
        assertEquals(identities, mapper.readValue(mapper.writeValueAsString(identities), IdentityDataMap.class));
    }

    @Test
    void storedTitleSurvivesJsonRoundTripWithoutChangingSourceIdentity() {
        var identity = new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, UUID.randomUUID(), "metadata", "user-123", null,
                Map.of("name", "Erika Muster"), 17, Map.of("mailbox", "reference")
        );
        var titledIdentity = identity.withTitle("Antragstellende Person");
        var mapper = JsonMapperTestUtils.createMapper();

        assertNull(identity.title());
        assertEquals("Antragstellende Person", titledIdentity.title());
        assertEquals(identity, titledIdentity.withTitle(null));
        assertEquals(titledIdentity, mapper.readValue(mapper.writeValueAsString(titledIdentity), IdentityData.class));
    }

    @Test
    void providerIdentityRequiresUniqueIdFromIdentityProvider() {
        var providerKey = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, providerKey, "metadata", null, null,
                Map.of(), null, Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, providerKey, "metadata", "   ", null,
                Map.of(), null, Map.of()
        ));
    }

    @Test
    void emailIdentityHasNoUniqueIdFromIdentityProvider() {
        var identity = new IdentityData(
                "session", "contact", IdentityType.Email, null, null, null, "person@example.org",
                Map.of(), null, Map.of()
        );

        assertNull(identity.uniqueIdFromIdentityProvider());
        assertThrows(IllegalArgumentException.class, () -> new IdentityData(
                "session", "contact", IdentityType.Email, null, null, "provider-user-123", "person@example.org",
                Map.of(), null, Map.of()
        ));
    }

    @Test
    void cacheConversionPreservesUniqueIdFromIdentityProvider() {
        var cacheEntity = new IdentityCacheEntity(
                "cache", "session", 42, null, IdentityType.IdentityProvider, UUID.randomUUID(), "applicant",
                "metadata", null, "https://example.org", "state", Map.of("sub", "provider-user-123"), null, null
        ).setUniqueIdFromIdentityProvider("provider-user-123");

        var identity = IdentityData.from(cacheEntity);

        assertEquals("provider-user-123", identity.uniqueIdFromIdentityProvider());
    }
}
