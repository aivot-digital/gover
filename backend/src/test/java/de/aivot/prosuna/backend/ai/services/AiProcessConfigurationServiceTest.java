package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.*;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.*;
import de.aivot.prosuna.backend.elements.models.input.*;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiProcessConfigurationServiceTest {
    private final tools.jackson.databind.json.JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final ProcessNodeService nodes = mock(ProcessNodeService.class);
    private final ProcessNodeDefinitionService definitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final AiProcessConfigurationService service = new AiProcessConfigurationService(mapper, nodes, definitions, new AiInputValueSchemaService(mapper));
    private final UserEntity user = new UserEntity().setId("owner");
    private final ProcessNodeEntity node = new ProcessNodeEntity().setConfiguration(new AuthoredElementValues());
    private final TextInputElement title = new TextInputElement();
    private final ReplicatingContainerLayoutElement repeat = new ReplicatingContainerLayoutElement();

    @BeforeEach
    void setUp() throws Exception {
        title.setId("title/~");
        title.setInputModePolicy(new InputModePolicy(List.of(InputMode.values()), InputMode.Literal, List.of(InputVariableSource.ProcessData)));
        repeat.setId("rows");
        var child = new NumberInputElement(); child.setId("count");
        repeat.setChildren(List.of(child));
        when(definitions.getProcessNodeDefinition(any(ProcessNodeEntity.class))).thenReturn(Optional.of(definition));
        when(nodes.getConfigLayoutElement(any(), any(), eq(user))).thenReturn(new ConfigLayoutElement().setChildren(List.of(title, repeat)));
        when(nodes.deriveConfigurationForAuthoring(any(), any(), eq(user), any(), any())).thenReturn(new DerivedRuntimeElementData());
    }

    @Test
    void preservesOmittedValuesAndDistinguishesNullFromRemoval() throws Exception {
        node.getConfiguration().putLiteral("rows", List.of());
        var changes = new LinkedHashMap<String, Object>();
        changes.put("/title~1~0", Map.of("type", "Literal", "value", "Title"));
        var result = service.patch(node, user, changes, List.of());
        assertThat(result).containsKeys("rows", "title/~");
        assertThat(node.getConfiguration()).doesNotContainKey("title/~");
        node.setConfiguration(result);
        var nullValue = mapper.readValue("{\"type\":\"Literal\",\"value\":null}", Map.class);
        result = service.patch(node, user, Map.of("/title~1~0", nullValue), List.of());
        assertThat(result.get("title/~")).isEqualTo(new LiteralAuthoredInputValue(null));
        node.setConfiguration(result);
        assertThat(service.patch(node, user, Map.of(), List.of("/title~1~0"))).containsKey("rows").doesNotContainKey("title/~");
    }

    @Test
    void acceptsDynamicModesButRejectsDisallowedSourcesAndMalformedEnvelopes() throws Exception {
        for (var json : List.of(
                "{\"type\":\"Variable\",\"reference\":{\"source\":\"ProcessData\",\"path\":\"counter\"}}",
                "{\"type\":\"NoCode\",\"operand\":{\"type\":\"NoCodeStaticValue\",\"value\":\"hello\"}}",
                "{\"type\":\"LowCode\",\"code\":\"return 'hello';\"}")) {
            assertThat(service.patch(node, user, Map.of("/title~1~0", mapper.readValue(json, Map.class)), List.of())).containsKey("title/~");
        }
        assertThatThrownBy(() -> service.patch(node, user, Map.of("/title~1~0", Map.of("type", "Variable", "reference", Map.of("source", "ElementData", "path", "x"))), List.of())).isInstanceOf(ResponseException.class);
        assertThatThrownBy(() -> service.patch(node, user, Map.of("/title~1~0", "raw"), List.of())).isInstanceOf(ResponseException.class);
        assertThatThrownBy(() -> service.patch(node, user, Map.of("/missing", Map.of("type", "Literal", "value", "x")), List.of())).isInstanceOf(ResponseException.class);
    }

    @Test
    void addressesRepeatedChildrenAndRejectsUnknownFieldsAtomically() throws Exception {
        var batch = new LinkedHashMap<String, Object>();
        batch.put("/rows", Map.of("type", "Literal", "value", List.of(Map.of("values", Map.of()))));
        batch.put("/rows/value/0/values/count", Map.of("type", "Literal", "value", 3));
        var result = service.patch(node, user, batch, List.of());
        assertThat(mapper.valueToTree(result).at("/rows/value/0/values/count/value").asInt()).isEqualTo(3);
        node.setConfiguration(result);
        assertThat(service.fields(node, user)).anyMatch(f -> f.valuePath().equals("/rows/value/*/values/count") && f.template());
        batch.put("/rows/value/0/values/missing", Map.of("type", "Literal", "value", 2));
        assertThatThrownBy(() -> service.patch(node, user, batch, List.of())).isInstanceOf(ResponseException.class);
        assertThat(node.getConfiguration()).isEqualTo(result);
    }

    @Test
    void exposesStateAndBoundsLargeValues() throws Exception {
        node.getConfiguration().putLiteral(title.getId(), "x".repeat(10000));
        var state = new ComputedElementState().setVisible(false).setDisabled(true).setError("Fehler");
        var derived = new DerivedRuntimeElementData(); derived.getElementStates().put(title.getId(), state);
        when(nodes.deriveConfigurationForAuthoring(any(), any(), eq(user), any(), any())).thenReturn(derived);
        var details = service.details(service.field(node, user, "/title~1~0"), node, 0);
        assertThat(details).containsEntry("visible", false).containsEntry("disabled", true).containsEntry("error", "Fehler");
        assertThat((AiToolResults.Value) details.get("value")).satisfies(value -> {
            assertThat(value.truncated()).isTrue(); assertThat(value.nextOffset()).isEqualTo(4000);
        });
    }
    @Test
    void rejectsNestedLiteralTypeErrorsAndInvalidEmbeddedForms() {
        var chips = new ChipInputElement();
        assertThatThrownBy(() -> service.validateEnvelope(chips, mapper.readTree("{\"type\":\"Literal\",\"value\":[42]}")))
                .isInstanceOf(ResponseException.class);
        var ui = new UiDefinitionInputElement().setElementType(de.aivot.prosuna.backend.enums.ElementType.FormLayout);
        assertThatThrownBy(() -> service.validateEnvelope(ui, mapper.readTree("{\"type\":\"Literal\",\"value\":{\"id\":\"root\",\"type\":0,\"children\":[{\"type\":999}]}}")))
                .isInstanceOf(ResponseException.class);
    }

}
