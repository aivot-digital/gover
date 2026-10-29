package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementConfigurationErrorsTest {
    private static final String TEXT_ERROR = "Sie fordern mehr Zeichen, als Sie maximal zulassen.";
    private static final String TABLE_ERROR = "Sie fordern mehr Zeilen, als Sie maximal zulassen.";

    @Test
    void shouldBeEmptyForValidTree() {
        var group = group("group", text("text", 1, 5));

        var errors = ElementConfigurationErrors.of(group);

        assertTrue(errors.isEmpty());
        assertEquals(List.of(), errors.describe(group));
    }

    @Test
    void shouldReportErrorsOfNestedChildrenByIdAndOmitValidChildren() {
        var table = new TableInputElement()
                .setFields(List.of(new TableInputElementColumn().setKey("name")))
                .setMinimumRequiredRows(3)
                .setMaximumRows(2);
        table.setId("table");
        table.setRequired(true);
        var root = group("root",
                text("valid", 1, 5),
                group("inner", text("invalid", 6, 5)),
                table
        );

        var errors = ElementConfigurationErrors.of(root);

        assertEquals(Map.of(), errors.properties());
        assertEquals(List.of("inner", "table"), List.copyOf(errors.children().keySet()));
        assertEquals(
                Map.of("minCharacters", List.of(TEXT_ERROR), "maxCharacters", List.of(TEXT_ERROR)),
                errors.children().get("inner").children().get("invalid").properties()
        );
        assertEquals(
                Map.of("minimumRequiredRows", List.of(TABLE_ERROR), "maximumRows", List.of(TABLE_ERROR)),
                errors.children().get("table").properties()
        );
    }

    @Test
    void shouldDescribeEachErrorOnceWithItsElement() {
        var labelled = text("labelled", 6, 5);
        labelled.setLabel("Vorname");
        var root = group("root", labelled, text("unlabelled", 6, 5));

        var messages = ElementConfigurationErrors.of(root).describe(root);

        assertEquals(List.of(
                "Element „Vorname“: " + TEXT_ERROR,
                "Element „unlabelled“: " + TEXT_ERROR
        ), messages);
    }

    @Test
    void shouldSerializeOnlyPropertiesAndChildren() {
        var root = group("root", text("text", 6, 5));

        var json = JsonMapperTestUtils
                .createMapper()
                .convertValue(ElementConfigurationErrors.of(root), Map.class);

        assertEquals(Set.of("properties", "children"), json.keySet());
    }

    @Test
    void shouldTreatNonPositiveMaximumAsNoLimit() {
        var table = new TableInputElement()
                .setFields(List.of(new TableInputElementColumn().setKey("name")))
                .setMinimumRequiredRows(3)
                .setMaximumRows(0);

        assertEquals(Map.of(), text("text", 6, 0).validateConfiguration());
        assertEquals(Map.of(), table.validateConfiguration());
    }

    private static TextInputElement text(String id, Integer minCharacters, Integer maxCharacters) {
        var text = new TextInputElement()
                .setMinCharacters(minCharacters)
                .setMaxCharacters(maxCharacters);
        text.setId(id);
        return text;
    }

    private static GroupLayoutElement group(String id, BaseFormElement... children) {
        var group = new GroupLayoutElement();
        group.setId(id);
        group.setChildren(new LinkedList<>(List.of(children)));
        return group;
    }
}
