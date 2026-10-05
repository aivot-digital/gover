package de.aivot.prosuna.backend.plugin.dtos;

import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginComponentDTOTest {
    @Test
    void from_ShouldMapAbstractAndDescriptionSeparately() {
        var component = mock(PluginComponent.class);
        when(component.getParentPluginKey()).thenReturn("de.aivot.test");
        when(component.getComponentKey()).thenReturn("example");
        when(component.getKey()).thenReturn("de.aivot.test.example");
        when(component.getComponentVersion()).thenReturn("2.1.3");
        when(component.getMajorVersion()).thenReturn(2);
        when(component.getComponentType()).thenReturn(PluginComponentType.OperatorProvider);
        when(component.getName()).thenReturn("Example component");
        when(component.getAbstract()).thenReturn("Concise plain-text abstract.");
        when(component.getDescription()).thenReturn("Detailed **Markdown** description.");
        when(component.getDocumentationUrl()).thenReturn("https://docs.example.com/components/example");
        when(component.getDeprecationNotice()).thenReturn("Use the replacement.");

        var result = PluginComponentDTO.from(component);

        assertEquals(
                new PluginComponentDTO(
                        "de.aivot.test",
                        "example",
                        "de.aivot.test.example",
                        "2.1.3",
                        2,
                        PluginComponentType.OperatorProvider,
                        "Example component",
                        "Concise plain-text abstract.",
                        "Detailed **Markdown** description.",
                        "https://docs.example.com/components/example",
                        "Use the replacement."
                ),
                result
        );
    }

    @Test
    void from_ShouldKeepMissingDocumentationUrlNull() {
        var component = mock(PluginComponent.class);

        var result = PluginComponentDTO.from(component);

        assertNull(result.documentationUrl());
    }
}
