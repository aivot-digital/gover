package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.models.AiProcessConfigurationChange;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiChatProcessToolsTest {
    private final AiChatProcessService service = mock(AiChatProcessService.class);

    @Test
    void exposesCompactTypedConfigurationChangesAndPassesThemToTheService() throws Exception {
        var callback = Arrays.stream(ToolCallbacks.from(new AiChatProcessTools(service)))
                .filter(candidate -> candidate.getToolDefinition().name().equals("aktualisiere-prozessknoten"))
                .findFirst().orElseThrow();
        var schema = JsonMapperTestUtils.createMapper().readTree(callback.getToolDefinition().inputSchema());

        assertThat(schema.at("/properties/configurationChanges/type").asString()).isEqualTo("array");
        assertThat(schema.at("/properties/configurationChanges/items/properties/mode/enum"))
                .isEqualTo(JsonMapperTestUtils.createMapper().readTree("[\"Literal\",\"Variable\",\"NoCode\",\"LowCode\"]"));
        assertThat(schema.at("/properties/configurationChanges/items/required").toString())
                .contains("valuePath", "mode", "value");
        assertThat(schema.toString()).doesNotContain("NoCodeExpression", "AuthoredInputValue");

        var scope = new AiProcessChatContext("owner", "session", 7, 2);
        when(service.updateNode(any(), anyInt(), any(), any(), any())).thenReturn(Map.of("saved", true));
        callback.call("{\"nodeId\":10,\"configurationChanges\":[{\"valuePath\":\"/amount\",\"mode\":\"Literal\",\"value\":1}]}",
                new ToolContext(Map.of(AiProcessChatContext.KEY, scope)));

        verify(service).updateNode(scope, 10, Map.of(),
                List.of(new AiProcessConfigurationChange("/amount", InputMode.Literal, 1)), List.of());
    }
}
