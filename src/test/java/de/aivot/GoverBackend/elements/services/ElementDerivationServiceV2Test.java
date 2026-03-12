package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.enums.EffectiveValueSource;
import de.aivot.GoverBackend.elements.models.*;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.BaseStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElementDerivationServiceV2Test {
    @Test
    void shouldProjectAuthoredValuesIntoRuntimeData() {
        var service = createService();
        var root = createSingleFieldLayout();

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", "hello");

        var result = service.derive(
                new ElementDerivationRequestV2(root, authoredValues, new ElementDerivationOptions()),
                new ElementDerivationLogger()
        );

        assertEquals("hello", result.getAuthoredValues().get("field"));
        assertEquals("hello", result.getEffectiveValues().get("field"));
        assertNull(result.getEffectiveValues().get("root"));
        assertNull(result.getEffectiveValues().get("step"));

        var fieldState = result.getElementStates().get("field");
        assertNotNull(fieldState);
        assertTrue(fieldState.getVisible());
        assertNull(fieldState.getError());
        assertEquals(EffectiveValueSource.Authored, fieldState.getValueSource());
    }

    @Test
    void shouldDeriveComputedValuesWithoutUsingTheLegacyService() {
        var service = createService();

        var sourceField = new TextInputElement();
        sourceField.setId("source");

        var derivedField = new TextInputElement();
        derivedField.setId("derived");
        derivedField.setTechnical(true);
        derivedField.setValue(new ElementValueFunctions().setNoCode(NoCodeReference.of("source")));

        var stepChildren = new LinkedList<BaseFormElement>();
        stepChildren.add(sourceField);
        stepChildren.add(derivedField);

        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(stepChildren);

        var rootChildren = new LinkedList<BaseStepElement>();
        rootChildren.add(step);

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(rootChildren);

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("source", "copied value");

        var result = service.derive(
                new ElementDerivationRequestV2(root, authoredValues, new ElementDerivationOptions()),
                new ElementDerivationLogger()
        );

        assertEquals("copied value", result.getEffectiveValues().get("source"));
        assertEquals("copied value", result.getEffectiveValues().get("derived"));
        assertEquals(
                EffectiveValueSource.Derived,
                result.getElementStates().get("derived").getValueSource()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDeriveReplicatingContainerRowsIntoNestedRuntimeData() {
        var service = createService();

        var rowField = new TextInputElement();
        rowField.setId("row_field");
        rowField.setRequired(true);

        var replicatingChildren = new LinkedList<BaseFormElement>();
        replicatingChildren.add(rowField);

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(replicatingChildren);

        var stepChildren = new LinkedList<BaseFormElement>();
        stepChildren.add(rows);

        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(stepChildren);

        var rootChildren = new LinkedList<BaseStepElement>();
        rootChildren.add(step);

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(rootChildren);

        var firstRow = new AuthoredElementValues();
        firstRow.put("row_field", "row-1");

        var secondRow = new AuthoredElementValues();

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("rows", List.of(firstRow, secondRow));

        var result = service.derive(
                new ElementDerivationRequestV2(root, authoredValues, new ElementDerivationOptions()),
                new ElementDerivationLogger()
        );

        var rowsState = result.getElementStates().get("rows");
        assertNotNull(rowsState);
        assertEquals(EffectiveValueSource.Authored, rowsState.getValueSource());
        assertNotNull(rowsState.getSubStates());
        assertEquals(2, rowsState.getSubStates().size());

        var rowEffectiveValues = (List<EffectiveElementValues>) result.getEffectiveValues().get("rows");
        assertNotNull(rowEffectiveValues);
        assertEquals(2, rowEffectiveValues.size());
        assertEquals("row-1", rowEffectiveValues.getFirst().get("row_field"));
        assertNull(rowEffectiveValues.getLast().get("row_field"));

        var firstRowState = rowsState.getSubStates().getFirst().get("row_field");
        assertNotNull(firstRowState);
        assertEquals(EffectiveValueSource.Authored, firstRowState.getValueSource());
        assertNull(firstRowState.getError());

        var secondRowState = rowsState.getSubStates().getLast().get("row_field");
        assertNotNull(secondRowState);
        assertEquals(EffectiveValueSource.Empty, secondRowState.getValueSource());
        assertEquals("Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.", secondRowState.getError());
    }

    private static ElementDerivationServiceV2 createService() {
        return new ElementDerivationServiceV2(
                new ElementOverrideDerivationService(),
                new ElementVisibilityDerivationService(),
                new ElementErrorDerivationService(),
                new ElementValueDerivationService(),
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of())
        );
    }

    private static FormLayoutElement createSingleFieldLayout() {
        var field = new TextInputElement();
        field.setId("field");

        var stepChildren = new LinkedList<BaseFormElement>();
        stepChildren.add(field);

        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(stepChildren);

        var rootChildren = new LinkedList<BaseStepElement>();
        rootChildren.add(step);

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(rootChildren);
        return root;
    }
}
