package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.ai.models.AiProcessConfigurationChange;
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
        var result = service.patch(node, user,
                List.of(change("/title~1~0", InputMode.Literal, "Title")), List.of()).configuration();
        assertThat(result).containsKeys("rows", "title/~");
        assertThat(node.getConfiguration()).doesNotContainKey("title/~");
        node.setConfiguration(result);
        result = service.patch(node, user,
                Collections.singletonList(change("/title~1~0", InputMode.Literal, null)), List.of()).configuration();
        assertThat(result.get("title/~")).isEqualTo(new LiteralAuthoredInputValue(null));
        node.setConfiguration(result);
        assertThat(service.patch(node, user, List.of(), List.of("/title~1~0")).configuration())
                .containsKey("rows").doesNotContainKey("title/~");
    }

    @Test
    void acceptsDynamicModesButRejectsDisallowedSourcesAndMalformedEnvelopes() throws Exception {
        var changes = List.of(
                change("/title~1~0", InputMode.Variable, Map.of("source", "ProcessData", "path", "counter")),
                change("/title~1~0", InputMode.NoCode, Map.of("type", "NoCodeStaticValue", "value", "hello")),
                change("/title~1~0", InputMode.LowCode, "return 'hello';"));
        for (var change : changes) {
            assertThat(service.patch(node, user, List.of(change), List.of()).configuration()).containsKey("title/~");
        }
        assertThat(service.patch(node, user, List.of(change("/title~1~0", InputMode.Variable,
                Map.of("source", "ElementData", "path", "x"))), List.of()).errors())
                .extracting(AiProcessConfigurationService.ConfigurationError::code)
                .containsExactly("VARIABLE_SOURCE_NOT_ALLOWED");
        assertThat(service.patch(node, user, List.of(change("/missing", InputMode.Literal, "x")), List.of()).errors())
                .extracting(AiProcessConfigurationService.ConfigurationError::code)
                .containsExactly("FIELD_NOT_AVAILABLE");
    }

    @Test
    void addressesRepeatedChildrenAndRejectsUnknownFieldsAtomically() throws Exception {
        var batch = List.of(
                change("/rows", InputMode.Literal, List.of(Map.of("values", Map.of()))),
                change("/rows/value/0/values/count", InputMode.Literal, 3));
        var result = service.patch(node, user, batch, List.of()).configuration();
        assertThat(mapper.valueToTree(result).at("/rows/value/0/values/count/value").asInt()).isEqualTo(3);
        node.setConfiguration(result);
        assertThat(service.fields(node, user)).anyMatch(f -> f.valuePath().equals("/rows/value/*/values/count") && f.template());
        var invalid = new ArrayList<>(batch);
        invalid.add(change("/rows/value/0/values/missing", InputMode.Literal, 2));
        assertThat(service.patch(node, user, invalid, List.of()).valid()).isFalse();
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
        assertThat(details).containsEntry("literalValueType", "string").containsKeys("literalValueSchema", "writeContract");
        assertThat((AiToolResults.Value) details.get("currentValue")).satisfies(value -> {
            assertThat(value.truncated()).isTrue(); assertThat(value.nextOffset()).isEqualTo(4000);
        });
    }

    @Test
    void reportsAllTypeErrorsWithoutMutatingTheNode() throws Exception {
        node.getConfiguration().putLiteral("rows", List.of(Map.of("values", Map.of())));
        var before = mapper.writeValueAsString(node.getConfiguration());

        var result = service.patch(node, user, List.of(
                change("/title~1~0", InputMode.Literal, 42),
                change("/rows/value/0/values/count", InputMode.Literal, "3"),
                change("/missing", InputMode.Literal, "x")
        ), List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting(AiProcessConfigurationService.ConfigurationError::code)
                .containsExactly("INVALID_LITERAL_TYPE", "INVALID_LITERAL_TYPE", "FIELD_NOT_AVAILABLE");
        assertThat(result.errors().get(1)).satisfies(error -> {
            assertThat(error.valuePath()).isEqualTo("/rows/value/0/values/count");
            assertThat(error.expectedJsonTypes()).contains("number");
            assertThat(error.actualJsonType()).isEqualTo("string");
        });
        assertThat(mapper.writeValueAsString(node.getConfiguration())).isEqualTo(before);
    }

    @Test
    void rejectsDuplicateAndConflictingPaths() throws Exception {
        var result = service.patch(node, user, List.of(
                change("/title~1~0", InputMode.Literal, "first"),
                change("/title~1~0", InputMode.Literal, "second")
        ), List.of("/title~1~0"));

        assertThat(result.errors()).extracting(AiProcessConfigurationService.ConfigurationError::code)
                .containsExactly("SET_AND_REMOVE", "DUPLICATE_PATH");
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

    private static AiProcessConfigurationChange change(String path, InputMode mode, Object value) {
        return new AiProcessConfigurationChange(path, mode, value);
    }

}
