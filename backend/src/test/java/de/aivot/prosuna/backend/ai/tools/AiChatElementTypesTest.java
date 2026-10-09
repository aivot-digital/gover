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
    private final AiChatElementSchemaTools tools = new AiChatElementSchemaTools(JsonMapperTestUtils.createMapper(), new de.aivot.prosuna.backend.ai.services.AiInputValueSchemaService(JsonMapperTestUtils.createMapper()));

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
    void exposesPaginatedSearchWithOptionalArguments() {
        var callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("liste-verfuegbare-elemente"))
                .findFirst()
                .orElseThrow();
        var schema = JsonMapperTestUtils.createMapper().readTree(callback.getToolDefinition().inputSchema());

        assertThat(schema.path("properties").propertyNames()).containsExactlyInAnyOrder("query", "offset", "limit");
        assertThat(schema.path("required").isEmpty()).isTrue();
        var firstPage = JsonMapperTestUtils.createMapper().readTree(callback.call("{}"));
        assertThat(firstPage.path("items")).hasSize(20);
        assertThat(firstPage.path("nextOffset").asInt()).isEqualTo(20);
        var lastPage = JsonMapperTestUtils.createMapper().readTree(callback.call("{\"offset\":40}"));
        assertThat(lastPage.path("items")).hasSize(ElementType.values().length - 40);
        assertThat(lastPage.path("nextOffset").isNull()).isTrue();
        var found = JsonMapperTestUtils.createMapper().readTree(callback.call("{\"query\":\"Schlagwörter\"}"));
        assertThat(found.path("items")).hasSize(1);
        verifyNoInteractions(repository);
    }
}
