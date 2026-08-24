package de.aivot.prosuna.backend.plugin.dtos;

import de.aivot.prosuna.backend.plugin.models.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginDTOTest {
    @Test
    void from_ShouldMapDocumentationUrl() {
        var plugin = mock(Plugin.class);
        when(plugin.getDocumentationUrl()).thenReturn("https://docs.example.com/plugins/example");

        var result = PluginDTO.from(plugin, List.of());

        assertEquals("https://docs.example.com/plugins/example", result.documentationUrl());
    }

    @Test
    void from_ShouldKeepMissingDocumentationUrlNull() {
        var plugin = mock(Plugin.class);

        var result = PluginDTO.from(plugin, List.of());

        assertNull(result.documentationUrl());
    }
}
