package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.elements.models.input.LowCodeAuthoredInputValue;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AiProcessFormServiceTest {
    private final tools.jackson.databind.json.JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final AiProcessFormService service = new AiProcessFormService(mapper);
    private final UiDefinitionInputElement input = new UiDefinitionInputElement().setElementType(ElementType.FormLayout);
    private final AiProcessConfigurationService.Field field = new AiProcessConfigurationService.Field(input, "/children/0", "/ui", new ComputedElementState(), false);
    private final ProcessNodeEntity node = new ProcessNodeEntity().setConfiguration(new AuthoredElementValues());

    @Test
    void buildsMovesConfiguresAndDeletesPersistableFormWithoutReplacingUnrelatedValues() throws Exception {
        node.getConfiguration().putLiteral("other", "Keep");
        service.edit(node, field, "create", "", null, 0, Map.of());
        service.edit(node, field, "create", "", "", 1, Map.of("title", "Abschnitt"));
        service.edit(node, field, "create", "", "", 1, Map.of("title", "Zweiter Abschnitt"));
        service.edit(node, field, "create", "", "/children/0", 15, Map.of("label", "Name", "required", true));
        var result = service.edit(node, field, "move", "/children/0/children/0", "/children/1", null, Map.of());
        assertThat(mapper.valueToTree(result).path("path").asString()).isEqualTo("/children/1/children/0");
        service.edit(node, field, "update", "/children/1/children/0", null, null, Map.of("label", "Hundename"));
        var restored = mapper.readValue(mapper.writeValueAsString(node.getConfiguration()), AuthoredElementValues.class);
        assertThat(mapper.valueToTree(restored).at("/ui/value/children/1/children/0/label").asString()).isEqualTo("Hundename");
        assertThat(restored.getLiteral("other")).isEqualTo("Keep");
        service.edit(node, field, "delete", "/children/0", null, null, Map.of());
        assertThat(mapper.valueToTree(node.getConfiguration()).at("/ui/value/children").size()).isEqualTo(1);
        assertThat(mapper.valueToTree(service.read(node, field, "/children/0", null, null)).toString()).doesNotContain("Hundename");
    }

    @Test
    void rejectsWrongRootTypeDynamicValuesImmutableIdsAndCycles() throws Exception {
        assertThatThrownBy(() -> service.edit(node, field, "create", "", null, 15, Map.of())).isInstanceOf(ResponseException.class);
        assertThat(node.getConfiguration()).isEmpty();
        service.edit(node, field, "create", "", null, 0, Map.of());
        service.edit(node, field, "create", "", "", 1, Map.of());
        assertThatThrownBy(() -> service.edit(node, field, "update", "", null, null, Map.of("id", "client-id"))).isInstanceOf(ResponseException.class);
        assertThatThrownBy(() -> service.edit(node, field, "move", "/children/0", "/children/0", null, Map.of())).isInstanceOf(ResponseException.class);
        node.getConfiguration().put("ui", new LowCodeAuthoredInputValue("return {};"));
        assertThatThrownBy(() -> service.read(node, field, null, null, null)).isInstanceOf(ResponseException.class);
    }
}
