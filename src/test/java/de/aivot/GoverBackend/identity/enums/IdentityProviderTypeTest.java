package de.aivot.GoverBackend.identity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityProviderTypeTest {

    @Test
    void matches() {
        // Test matching the same enum instance
        assertTrue(IdentityProviderType.BayernId.matches(IdentityProviderType.BayernId));

        // Test matching different enum instances
        assertFalse(IdentityProviderType.BayernId.matches(IdentityProviderType.BundId));

        // Test matching with null
        assertFalse(IdentityProviderType.BayernId.matches(null));

        // Test matching with an object of a different type
        assertFalse(IdentityProviderType.BayernId.matches("SomeString"));
    }
}