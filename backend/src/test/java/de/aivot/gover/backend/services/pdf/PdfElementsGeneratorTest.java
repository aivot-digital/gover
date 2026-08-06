package de.aivot.gover.backend.services.pdf;

import de.aivot.gover.backend.elements.models.ComputedElementState;
import de.aivot.gover.backend.elements.models.ComputedElementSubState;
import de.aivot.gover.backend.elements.models.ComputedElementStates;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.enums.TableColumnDataType;
import de.aivot.gover.backend.services.pdf.PdfElementsGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfElementsGeneratorTest {
    @Test
    void blankPrintCreatesFivePlaceholderRowsForTableWithoutLimits() {
        var table = new TableInputElement();
        table.setLabel("Tabelle");
        table.setFields(List.of(
                new TableInputElementColumn()
                        .setKey("first_name")
                        .setLabel("Vorname")
                        .setDatatype(TableColumnDataType.String)
        ));
        var root = createRoot(table);

        var step = PdfElementsGenerator.generatePdfElements(root, null, true).getFirst();
        var tableElement = step.children().getFirst();
        var rows = (List<?>) tableElement.value();
        var firstRow = (Map<?, ?>) rows.getFirst();

        assertEquals(5, rows.size());
        assertTrue(firstRow.containsKey("first_name"));
        assertFalse(firstRow.containsKey("Vorname"));
    }

    @Test
    void blankPrintCreatesFivePlaceholderDatasetsForReplicatingContainerWithoutLimits() {
        var child = new TextInputElement();
        child.setLabel("Name");
        var replicatingContainer = new ReplicatingContainerLayoutElement()
                .setChildren(List.of(child));
        replicatingContainer.setLabel("Personen");
        var root = createRoot(replicatingContainer);

        var step = PdfElementsGenerator.generatePdfElements(root, null, true).getFirst();
        var replicatingContainerElement = step.children().getFirst();

        assertEquals(5, ((List<?>) replicatingContainerElement.value()).size());
        assertEquals(5, replicatingContainerElement.children().size());
    }

    @Test
    void blankPrintUsesPositiveMaximumForReplicatingContainerPlaceholders() {
        var child = new TextInputElement();
        child.setLabel("Name");
        var replicatingContainer = new ReplicatingContainerLayoutElement()
                .setMaximumSets(2)
                .setChildren(List.of(child));
        var root = createRoot(replicatingContainer);

        var step = PdfElementsGenerator.generatePdfElements(root, null, true).getFirst();
        var replicatingContainerElement = step.children().getFirst();

        assertEquals(2, ((List<?>) replicatingContainerElement.value()).size());
        assertEquals(2, replicatingContainerElement.children().size());
    }

    @Test
    void blankPrintUsesMinimumWhenItExceedsDefaultPlaceholders() {
        var table = new TableInputElement()
                .setMinimumRequiredRows(6)
                .setFields(List.of(
                        new TableInputElementColumn()
                                .setKey("name")
                                .setLabel("Name")
                                .setDatatype(TableColumnDataType.String)
                ));
        var root = createRoot(table);

        var step = PdfElementsGenerator.generatePdfElements(root, null, true).getFirst();
        var tableElement = step.children().getFirst();

        assertEquals(6, ((List<?>) tableElement.value()).size());
    }

    @Test
    void blankPrintUsesRuntimeOverridesAndKeepsTablePlaceholders() {
        var select = new SelectInputElement();
        select.setLabel("Ort");
        select.setId("city");
        var resolvedSelect = new SelectInputElement();
        resolvedSelect.setLabel("Ort");
        resolvedSelect.setOptions(List.of(
                SelectInputElementOption.of("001", "Berlin")
        ));
        resolvedSelect.setId("city");
        var table = new TableInputElement()
                .setFields(List.of(
                        new TableInputElementColumn()
                                .setKey("name")
                                .setLabel("Name")
                                .setDatatype(TableColumnDataType.String)
                ));
        var states = new ComputedElementStates();
        states.put("city", new ComputedElementState().setOverride(resolvedSelect));
        var root = createRoot(select, table);

        var step = PdfElementsGenerator
                .generatePdfElements(root, new DerivedRuntimeElementData().setElementStates(states), true, true)
                .getFirst();
        var selectElement = assertInstanceOf(SelectInputElement.class, step.children().getFirst().element());

        assertEquals("Berlin", selectElement.getOptions().getFirst().getLabel());
        assertEquals(5, ((List<?>) step.children().get(1).value()).size());
    }

    @Test
    void blankPrintUsesReplicatingContainerSubStateOverridesForPlaceholderRows() {
        var select = new SelectInputElement();
        select.setLabel("Ort");
        select.setId("city");
        var resolvedSelect = new SelectInputElement();
        resolvedSelect.setLabel("Ort");
        resolvedSelect.setOptions(List.of(
                SelectInputElementOption.of("001", "Berlin")
        ));
        resolvedSelect.setId("city");
        var replicatingContainer = new ReplicatingContainerLayoutElement()
                .setChildren(List.of(select));
        replicatingContainer.setId("people");
        var rowStates = new ComputedElementStates();
        rowStates.put("city", new ComputedElementState().setOverride(resolvedSelect));
        var states = new ComputedElementStates();
        states.put("people", new ComputedElementState().setSubStates(List.of(ComputedElementSubState.of("row-1", rowStates))));
        var root = createRoot(replicatingContainer);

        var step = PdfElementsGenerator
                .generatePdfElements(root, new DerivedRuntimeElementData().setElementStates(states), true, true)
                .getFirst();
        var firstPlaceholderRow = step.children().getFirst().children().getFirst();
        var selectElement = assertInstanceOf(SelectInputElement.class, firstPlaceholderRow.children().getFirst().element());

        assertEquals("Berlin", selectElement.getOptions().getFirst().getLabel());
    }

    @Test
    void rendersReplicatingContainerRowsFromValueObjects() {
        var child = new TextInputElement();
        child.setId("name");
        child.setLabel("Name");
        var replicatingContainer = new ReplicatingContainerLayoutElement()
                .setChildren(List.of(child));
        replicatingContainer.setId("people");
        var root = createRoot(replicatingContainer);

        var rowValues = new AuthoredElementValues();
        rowValues.put("name", "Ada");
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(
                new ReplicatingContainerLayoutElementValue().setValues(rowValues)
        ));

        var step = PdfElementsGenerator
                .generatePdfElements(root, new DerivedRuntimeElementData().setEffectiveValues(effectiveValues), true, false)
                .getFirst();
        var peopleElement = step.children().getFirst();
        var firstRow = peopleElement.children().getFirst();

        assertEquals("Ada", firstRow.children().getFirst().value());
    }

    private FormLayoutElement createRoot(BaseFormElement... children) {
        return new FormLayoutElement()
                .setChildren(List.of(
                        new GenericStepElement()
                                .setChildren(List.of(children))
                ));
    }
}
