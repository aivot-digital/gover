package de.aivot.GoverBackend.identity.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityAdditionalParameterTest {

    @Test
    void testEquals() {
        IdentityAdditionalParameter param1 = new IdentityAdditionalParameter()
                .setKey("key1")
                .setValue("value1");

        IdentityAdditionalParameter param2 = new IdentityAdditionalParameter()
                .setKey("key1")
                .setValue("value1");

        IdentityAdditionalParameter param3 = new IdentityAdditionalParameter()
                .setKey("key2")
                .setValue("value2");

        assertEquals(param1, param2); // Same attributes
        assertNotEquals(param1, param3); // Different attributes
        assertNotEquals(null, param1); // Null comparison
        assertNotEquals(new Object(), param1); // Different class comparison
    }

    @Test
    void testHashCode() {
        IdentityAdditionalParameter param1 = new IdentityAdditionalParameter()
                .setKey("key1")
                .setValue("value1");

        IdentityAdditionalParameter param2 = new IdentityAdditionalParameter()
                .setKey("key1")
                .setValue("value1");

        IdentityAdditionalParameter param3 = new IdentityAdditionalParameter()
                .setKey("key2")
                .setValue("value2");

        assertEquals(param1.hashCode(), param2.hashCode()); // Same attributes
        assertNotEquals(param1.hashCode(), param3.hashCode()); // Different attributes
    }
}