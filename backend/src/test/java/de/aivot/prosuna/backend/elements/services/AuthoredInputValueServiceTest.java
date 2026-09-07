package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.EffectiveReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.VariableAuthoredInputValue;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthoredInputValueServiceTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AuthoredInputValueService service = new AuthoredInputValueService(jsonMapper);

    @Test
    void shouldReadAndUpdateLiteralValues() {
        var values = new AuthoredElementValues();
        service.putLiteral(values, "field", Map.of("nested", 3));

        assertEquals(Map.of("nested", 3), service.getLiteral(values, "field"));

        service.mapLiteral(values, "field", ignored -> "updated");

        assertEquals("updated", service.getLiteral(values, "field"));
    }

    @Test
    void shouldNotUnwrapDynamicValuesAsLiterals() {
        var values = new AuthoredElementValues();
        values.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "person.name", null)
        ));

        assertNull(service.getLiteral(values, "field"));
        assertThrows(IllegalStateException.class, values::toLiteralValues);
    }

    @Test
    void shouldTreatWrapperShapedRuntimeObjectsAsLiteralData() {
        var values = service.toLiteralAuthoredElementValues(Map.of(
                "field", Map.of("type", "Variable", "reference", Map.of("path", "untrusted"))
        ));

        var literal = assertInstanceOf(LiteralAuthoredInputValue.class, values.get("field"));
        assertEquals(Map.of("type", "Variable", "reference", Map.of("path", "untrusted")), literal.value());
    }

    @Test
    void shouldRebuildNestedAuthoredRowsFromEffectiveValues() {
        var text = new TextInputElement();
        text.setId("name");
        var nestedText = new TextInputElement();
        nestedText.setId("detail");
        var nestedContainer = new ReplicatingContainerLayoutElement();
        nestedContainer.setId("details");
        nestedContainer.setChildren(List.of(nestedText));
        var container = new ReplicatingContainerLayoutElement();
        container.setId("rows");
        container.setChildren(List.of(text, nestedContainer));
        var root = new GroupLayoutElement();
        root.setId("root");
        root.setChildren(List.of(container));

        var nestedValues = new EffectiveElementValues();
        nestedValues.put("detail", "nested");
        var rowValues = new EffectiveElementValues();
        rowValues.put("name", "Ada");
        rowValues.put("details", List.of(Map.of("id", "nested-row", "values", nestedValues)));
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("rows", List.of(
                new EffectiveReplicatingContainerLayoutElementValue()
                        .setId("row-1")
                        .setValues(rowValues)
        ));

        var authoredValues = service.toLiteralAuthoredElementValues(root, effectiveValues);
        var rows = (List<?>) authoredValues.getLiteral("rows");
        var row = assertInstanceOf(ReplicatingContainerLayoutElementValue.class, rows.getFirst());
        assertEquals("Ada", row.getValues().getLiteral("name"));
        var nestedRows = (List<?>) row.getValues().getLiteral("details");
        var nestedRow = assertInstanceOf(ReplicatingContainerLayoutElementValue.class, nestedRows.getFirst());
        assertEquals("nested", nestedRow.getValues().getLiteral("detail"));
    }

    @Test
    void shouldDeserializeThePolymorphicAuthoredContractWithJackson() throws Exception {
        var json = """
                {
                  "literal": {"type": "Literal", "value": 3},
                  "variable": {
                    "type": "Variable",
                    "reference": {"source": "ProcessData", "path": "person.name"}
                  }
                }
                """;

        var values = jsonMapper.readValue(json, AuthoredElementValues.class);

        assertEquals(3, ((Number) assertInstanceOf(LiteralAuthoredInputValue.class, values.get("literal")).value()).intValue());
        var variable = assertInstanceOf(VariableAuthoredInputValue.class, values.get("variable"));
        assertEquals(InputVariableSource.ProcessData, variable.reference().source());
        assertEquals("person.name", variable.reference().path());
    }

    @Test
    void shouldDeserializeNestedReplicatingRowsWithAuthoredChildValues() throws JacksonException {
        var json = """
                {
                  "rows": {
                    "type": "Literal",
                    "value": [{
                      "id": "row-1",
                      "values": {
                        "name": {"type": "Literal", "value": "Ada"}
                      }
                    }]
                  }
                }
                """;

        var values = jsonMapper.readValue(json, AuthoredElementValues.class);
        var rows = ReplicatingContainerLayoutElement._formatValue(values.getLiteral("rows"));

        var row = assertInstanceOf(ReplicatingContainerLayoutElementValue.class, rows.getFirst());
        assertEquals("row-1", row.getId());
        assertEquals("Ada", row.getValues().getLiteral("name"));
    }

    @Test
    void shouldRejectAnUnwrappedNullEntryDuringDeserialization() {
        assertThrows(JacksonException.class, () -> jsonMapper.readValue("{\"field\":null}", AuthoredElementValues.class));
    }
}
