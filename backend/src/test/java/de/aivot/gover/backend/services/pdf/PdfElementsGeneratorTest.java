package de.aivot.gover.backend.services.pdf;

import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.enums.TableColumnDataType;
import de.aivot.gover.backend.services.pdf.PdfElementsGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private FormLayoutElement createRoot(BaseFormElement child) {
        return new FormLayoutElement()
                .setChildren(List.of(
                        new GenericStepElement()
                                .setChildren(List.of(child))
                ));
    }
}
