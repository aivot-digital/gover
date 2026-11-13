package de.aivot.prosuna.backend.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {
    @Test
    void randomStringUsesTheRequestedLengthAndOnlyAlphanumericCharacters() {
        var value = StringUtils.randomString(64);

        assertEquals(64, value.length());
        // PKCE code verifiers may only contain unreserved URL characters.
        assertTrue(value.matches("[A-Za-z0-9]+"));
    }

    @Test
    void randomStringReturnsDifferentValues() {
        assertNotEquals(StringUtils.randomString(64), StringUtils.randomString(64));
    }
}
