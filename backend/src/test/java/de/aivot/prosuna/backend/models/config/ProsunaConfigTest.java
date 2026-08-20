package de.aivot.prosuna.backend.models.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProsunaConfigTest {
    @Test
    void shouldAcceptAbsoluteHttpSupportUrl() {
        var config = new ProsunaConfig();

        config.setSupportUrl(" https://support.example.test/portal ");

        assertEquals("https://support.example.test/portal", config.getSupportUrl());
    }

    @Test
    void shouldTreatBlankSupportUrlAsNotConfigured() {
        var config = new ProsunaConfig();

        config.setSupportUrl("  ");

        assertNull(config.getSupportUrl());
    }

    @Test
    void shouldRejectUnsafeSupportUrl() {
        var config = new ProsunaConfig();

        assertThrows(IllegalArgumentException.class, () -> config.setSupportUrl("javascript:alert(1)"));
    }
}
