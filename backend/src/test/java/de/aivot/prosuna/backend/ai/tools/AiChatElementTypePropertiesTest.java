package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiChatElementTypePropertiesTest {
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(JsonMapperTestUtils.createMapper(), repository);

    @Test
    void listsInheritedAndJsonNamedPropertiesWithoutNestedDetails() {
        var properties = tools.listElementTypeProperties(ElementType.Text.getKey()).lines().toList();

        assertThat(properties)
                .contains("- id", "- type", "- name", "- label", "- isMultiline", "- pattern", "- visibility")
                .doesNotContain("- multiline", "- regex", "- message", "- conditionSet", "- class", "- COPY_VALUE_TEMPLATE_PLACEHOLDER")
                .doesNotHaveDuplicates()
                .allSatisfy(line -> assertThat(line).matches("- [A-Za-z][A-Za-z0-9_]*"));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownTypesAndTypesWithoutAnElementClass() {
        var error = "Die Eigenschaften konnten nicht abgerufen werden: Der Formularelementtyp wird nicht unterstützt.";

        assertThat(tools.listElementTypeProperties(-1)).isEqualTo(error);
        assertThat(tools.listElementTypeProperties(ElementType.SubmittedStep.getKey())).isEqualTo(error);
        verifyNoInteractions(repository);
    }
}
