package de.aivot.GoverBackend.identity.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityAttributeMappingTest {

    @Test
    void testEquals() {
        IdentityAttributeMapping mapping1 = new IdentityAttributeMapping()
                .setLabel("Email")
                .setDescription("The user's email address")
                .setKeyInData("email");

        IdentityAttributeMapping mapping2 = new IdentityAttributeMapping()
                .setLabel("Email")
                .setDescription("The user's email address")
                .setKeyInData("email");

        IdentityAttributeMapping mapping3 = new IdentityAttributeMapping()
                .setLabel("Name")
                .setDescription("The user's full name")
                .setKeyInData("name");

        assertEquals(mapping1, mapping2); // Same attributes
        assertNotEquals(mapping1, mapping3); // Different attributes
        assertNotEquals(null, mapping1); // Null comparison
        assertNotEquals(new Object(), mapping1); // Different class comparison
    }

    @Test
    void testHashCode() {
        IdentityAttributeMapping mapping1 = new IdentityAttributeMapping()
                .setLabel("Email")
                .setDescription("The user's email address")
                .setKeyInData("email");

        IdentityAttributeMapping mapping2 = new IdentityAttributeMapping()
                .setLabel("Email")
                .setDescription("The user's email address")
                .setKeyInData("email");

        IdentityAttributeMapping mapping3 = new IdentityAttributeMapping()
                .setLabel("Name")
                .setDescription("The user's full name")
                .setKeyInData("name");

        assertEquals(mapping1.hashCode(), mapping2.hashCode()); // Same attributes
        assertNotEquals(mapping1.hashCode(), mapping3.hashCode()); // Different attributes
    }
}