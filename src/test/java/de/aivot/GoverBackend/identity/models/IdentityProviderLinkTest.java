package de.aivot.GoverBackend.identity.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderLinkTest {

    @Test
    void testEquals() {
        IdentityProviderLink link1 = new IdentityProviderLink()
                .setIdentityProviderKey("provider1")
                .setAdditionalScopes(List.of("scope1", "scope2"));

        IdentityProviderLink link2 = new IdentityProviderLink()
                .setIdentityProviderKey("provider1")
                .setAdditionalScopes(List.of("scope1", "scope2"));

        IdentityProviderLink link3 = new IdentityProviderLink()
                .setIdentityProviderKey("provider2")
                .setAdditionalScopes(List.of("scope3"));

        IdentityProviderLink link4 = new IdentityProviderLink()
                .setIdentityProviderKey(null)
                .setAdditionalScopes(null);

        assertEquals(link1, link2); // Same attributes
        assertNotEquals(link1, link3); // Different attributes
        assertNotEquals(link1, link4); // Null attributes
        assertNotEquals(null, link1); // Null comparison
        assertNotEquals(new Object(), link1); // Different class comparison
    }

    @Test
    void testHashCode() {
        IdentityProviderLink link1 = new IdentityProviderLink()
                .setIdentityProviderKey("provider1")
                .setAdditionalScopes(List.of("scope1", "scope2"));

        IdentityProviderLink link2 = new IdentityProviderLink()
                .setIdentityProviderKey("provider1")
                .setAdditionalScopes(List.of("scope1", "scope2"));

        IdentityProviderLink link3 = new IdentityProviderLink()
                .setIdentityProviderKey("provider2")
                .setAdditionalScopes(List.of("scope3"));

        IdentityProviderLink link4 = new IdentityProviderLink()
                .setIdentityProviderKey(null)
                .setAdditionalScopes(null);

        assertEquals(link1.hashCode(), link2.hashCode()); // Same attributes
        assertNotEquals(link1.hashCode(), link3.hashCode()); // Different attributes
        assertNotEquals(link1.hashCode(), link4.hashCode()); // Null attributes
    }
}