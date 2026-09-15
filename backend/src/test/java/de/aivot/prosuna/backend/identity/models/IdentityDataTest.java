package de.aivot.prosuna.backend.identity.models;

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
