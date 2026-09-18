package de.aivot.prosuna.backend.storage.controllers;

import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageProviderDefinitionControllerTest {
    @Test
    void definitionDto_ShouldMapAbstractAndDescriptionSeparately() throws Exception {
        @SuppressWarnings("unchecked")
        var definition = (StorageProviderDefinition<Object>) mock(StorageProviderDefinition.class);
        when(definition.getKey()).thenReturn("de.aivot.test.storage");
        when(definition.getMajorVersion()).thenReturn(3);
        when(definition.getName()).thenReturn("Test storage");
        when(definition.getAbstract()).thenReturn("Concise storage abstract.");
        when(definition.getDescription()).thenReturn("Detailed **storage** description.");
        when(definition.getDocumentationUrl()).thenReturn("https://docs.example.com/storage/test");
        when(definition.getSupportsMetadataAttributes()).thenReturn(true);
        when(definition.getProviderConfigLayout()).thenReturn(null);

        var result = StorageProviderDefinitionController.StorageProviderDefinitionDTO.from(definition);

        assertEquals("de.aivot.test.storage", result.key());
        assertEquals(3, result.version());
        assertEquals("Test storage", result.name());
        assertEquals("Concise storage abstract.", result.abstractDescription());
        assertEquals("Detailed **storage** description.", result.description());
        assertEquals("https://docs.example.com/storage/test", result.documentationUrl());
        assertEquals(true, result.supportsMetadataAttributes());
        assertNull(result.providerConfigLayout());
    }
}
