package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiChatElementTypesTest {
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(JsonMapperTestUtils.createMapper(), repository);

    @Test
    void listsEveryElementTypeWithItsNumericKeyAndDescription() {
        var result = tools.listElementTypes();

        for (var type : ElementType.values()) {
            assertThat(result).contains(
                    "Typ-Schlüssel = " + type.getKey(),
                    "Anzeigenamen = " + type.getDisplayName(),
                    "Beschreibung = " + type.getDescription()
            );
        }
        assertThat(result.lines()).hasSize(ElementType.values().length);
        verifyNoInteractions(repository);
    }

    @Test
    void exposesTheListAsAnArgumentlessKebabCaseTool() {
        var callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("liste-verfuegbare-elemente"))
                .findFirst()
                .orElseThrow();
        var schema = JsonMapperTestUtils.createMapper().readTree(callback.getToolDefinition().inputSchema());

        assertThat(schema.path("properties").isEmpty()).isTrue();
        assertThat(JsonMapperTestUtils.createMapper().readTree(callback.call("{}")).asString())
                .isEqualTo(tools.listElementTypes());
        verifyNoInteractions(repository);
    }
}
