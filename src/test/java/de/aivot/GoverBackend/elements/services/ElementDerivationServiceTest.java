package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.enums.EffectiveValueSource;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.ElementOverrideFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.BaseStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementDerivationServiceTest {
    @Test
    void shouldProjectAuthoredValuesIntoRuntimeData() {
        var field = new TextInputElement();
        field.setId("field");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", "hello");

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions()
        );

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
    void shouldMarkComputedValuesAsDerived() {
        var sourceField = new TextInputElement();
        sourceField.setId("source");

        var derivedField = new TextInputElement();
        derivedField.setId("derived");
        derivedField.setValue(new ElementValueFunctions().setNoCode(NoCodeReference.of("source")));

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("source", "copied value");

        var result = derive(
                createRoot(List.of(sourceField, derivedField)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("copied value", result.getEffectiveValues().get("source"));
        assertEquals("copied value", result.getEffectiveValues().get("derived"));
        assertEquals(
                EffectiveValueSource.Derived,
                result.getElementStates().get("derived").getValueSource()
        );
    }

    @Test
    void shouldExposeResolvedDestinationPathForSimpleDestinationKey() {
        var field = new TextInputElement();
        field.setId("field");
        field.setDestinationKey("person.first_name");

        var result = derive(
                createRoot(List.of(field)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertEquals("person.first_name", result.getElementStates().get("field").getDestinationPath());
    }

    @Test
    void shouldExposeResolvedDestinationPathForExplicitArrayIndex() {
        var field = new TextInputElement();
        field.setId("field");
        field.setDestinationKey("members.0.first_name");

        var result = derive(
                createRoot(List.of(field)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertEquals("members.0.first_name", result.getElementStates().get("field").getDestinationPath());
    }

    @Test
    void shouldExposeResolvedDestinationPathForReplicatingContainerChildrenWithContainerDestinationKey() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("first_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("payload.people");
        people.setChildren(new LinkedList<>(List.of(firstName)));

        var firstRow = new AuthoredElementValues();
        firstRow.put("rowFirstName", "Ada");

        var secondRow = new AuthoredElementValues();
        secondRow.put("rowFirstName", "Grace");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("people", List.of(firstRow, secondRow));

        var result = derive(
                createRoot(List.of(people)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("payload.people", result.getElementStates().get("people").getDestinationPath());
        assertEquals(
                "payload.people.0.first_name",
                result.getElementStates().get("people").getSubStates().get(0).get("rowFirstName").getDestinationPath()
        );
        assertEquals(
                "payload.people.1.first_name",
                result.getElementStates().get("people").getSubStates().get(1).get("rowFirstName").getDestinationPath()
        );
    }

    @Test
    void shouldExposeResolvedDestinationPathForWildcardInsideReplicatingContainerWithoutContainerDestinationKey() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("members.*.first_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setChildren(new LinkedList<>(List.of(firstName)));

        var firstRow = new AuthoredElementValues();
        firstRow.put("rowFirstName", "Ada");

        var secondRow = new AuthoredElementValues();
        secondRow.put("rowFirstName", "Grace");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("people", List.of(firstRow, secondRow));

        var result = derive(
                createRoot(List.of(people)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertNull(result.getElementStates().get("people").getDestinationPath());
        assertEquals(
                "members.0.first_name",
                result.getElementStates().get("people").getSubStates().get(0).get("rowFirstName").getDestinationPath()
        );
        assertEquals(
                "members.1.first_name",
                result.getElementStates().get("people").getSubStates().get(1).get("rowFirstName").getDestinationPath()
        );
    }

    @Test
    void shouldRetainBroadcastWildcardInDestinationPathOutsideReplicatingContainers() {
        var lastName = new TextInputElement();
        lastName.setId("rowLastName");
        lastName.setDestinationKey("last_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("members");
        people.setChildren(new LinkedList<>(List.of(lastName)));

        var sharedFirstName = new TextInputElement();
        sharedFirstName.setId("sharedFirstName");
        sharedFirstName.setDestinationKey("members.*.first_name");

        var firstRow = new AuthoredElementValues();
        firstRow.put("rowLastName", "Lovelace");

        var secondRow = new AuthoredElementValues();
        secondRow.put("rowLastName", "Hopper");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("people", List.of(firstRow, secondRow));
        authoredValues.put("sharedFirstName", "Ada");

        var result = derive(
                createRoot(List.of(people, sharedFirstName)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("members.*.first_name", result.getElementStates().get("sharedFirstName").getDestinationPath());
    }

    @Test
    void shouldUseOverrideDestinationKeyForResolvedDestinationPath() {
        var field = new TextInputElement();
        field.setId("field");
        field.setDestinationKey("person.first_name");
        field.setOverride(new ElementOverrideFunctions().setJavascriptCode(
                JavascriptCode.of("({ type: element.type, id: element.id, destinationKey: 'person.given_name' })")
        ));

        var result = derive(
                createRoot(List.of(field)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertEquals("person.given_name", result.getElementStates().get("field").getDestinationPath());
    }

    @Test
    void shouldSkipVisibilitiesForSkippedElementsAndChildren() {
        var child = new TextInputElement();
        child.setId("child");
        child.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        assertFalse(baselineResult.getElementStates().get("group").getVisible());
        assertFalse(baselineResult.getElementStates().get("child").getVisible());

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipVisibilitiesForElementIds(List.of("group"))
        );

        assertTrue(skippedResult.getElementStates().get("group").getVisible());
        assertTrue(skippedResult.getElementStates().get("child").getVisible());
    }

    @Test
    void shouldSkipOverridesForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setLabel("original");
        child.setOverride(new ElementOverrideFunctions().setJavascriptCode(
                JavascriptCode.of("({ type: element.type, id: element.id, label: 'overridden child' })")
        ));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        var overrideElement = assertInstanceOf(
                TextInputElement.class,
                baselineResult.getElementStates().get("child").getOverride()
        );
        assertEquals("overridden child", overrideElement.getLabel());

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipOverridesForElementIds(List.of("group"))
        );

        assertNull(skippedResult.getElementStates().get("child").getOverride());
    }

    @Test
    void shouldSkipValuesForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setDisabled(true);
        child.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("derived value")));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("child", "authored value");

        var baselineResult = derive(
                createRoot(List.of(group)),
                authoredValues,
                new ElementDerivationOptions()
        );
        assertEquals("derived value", baselineResult.getEffectiveValues().get("child"));

        var skippedResult = derive(
                createRoot(List.of(group)),
                authoredValues,
                new ElementDerivationOptions().setSkipValuesForElementIds(List.of("group"))
        );

        assertEquals("authored value", skippedResult.getEffectiveValues().get("child"));
        assertEquals(
                EffectiveValueSource.Authored,
                skippedResult.getElementStates().get("child").getValueSource()
        );
    }

    @Test
    void shouldSkipErrorsForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setRequired(true);

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        assertEquals(
                "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.",
                baselineResult.getElementStates().get("child").getError()
        );

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipErrorsForElementIds(List.of("group"))
        );

        assertNull(skippedResult.getElementStates().get("child").getError());
    }

    @Test
    void shouldClearDependentSelectValueWhenParentSelectIsInOuterScope() {
        var parent = createGroupedSelect("parent", null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A"),
                SelectInputElementOption.of("group_b", "Gruppe B")
        ));

        var child = createGroupedSelect("child", "parent", List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a"),
                SelectInputElementOption.of("option_b", "Option B", "group_b")
        ));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(child)));

        var rowValues = new AuthoredElementValues();
        rowValues.put("child", "option_a");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("parent", "group_b");
        authoredValues.put("rows", List.of(rowValues));

        var result = derive(
                createRoot(List.of(parent, rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(Map.class, effectiveRows.get(0));

        assertEquals("group_b", result.getEffectiveValues().get("parent"));
        assertNull(firstRow.get("child"));
        assertNull(result.getElementStates().get("rows").getSubStates().get(0).get("child").getError());
    }

    @Test
    void shouldClearDependentSelectValueWhenParentSelectIsInCurrentReplicatingRow() {
        var rowParent = createGroupedSelect("row_parent", null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A"),
                SelectInputElementOption.of("group_b", "Gruppe B")
        ));

        var rowChild = createGroupedSelect("row_child", "row_parent", List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a"),
                SelectInputElementOption.of("option_b", "Option B", "group_b")
        ));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(rowParent, rowChild)));

        var rowValues = new AuthoredElementValues();
        rowValues.put("row_parent", "group_b");
        rowValues.put("row_child", "option_a");

        var authoredValues = new AuthoredElementValues();
        authoredValues.put("rows", List.of(rowValues));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(Map.class, effectiveRows.get(0));

        assertEquals("group_b", firstRow.get("row_parent"));
        assertNull(firstRow.get("row_child"));
        assertNull(result.getElementStates().get("rows").getSubStates().get(0).get("row_child").getError());
    }

    private static DerivedRuntimeElementData derive(
            FormLayoutElement root,
            AuthoredElementValues authoredValues,
            ElementDerivationOptions options
    ) {
        return createService().derive(
                new ElementDerivationRequest(root, authoredValues, options),
                new ElementDerivationLogger()
        );
    }

    private static ElementDerivationService createService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of()),
                new ElementDataTransformService()
        );
    }

    private static SelectInputElement createGroupedSelect(
            String id,
            String dependsOnSelectFieldId,
            List<SelectInputElementOption> options
    ) {
        var field = new SelectInputElement();
        field.setId(id);
        field.setOptions(options);
        field.setDependsOnSelectFieldId(dependsOnSelectFieldId);
        return field;
    }

    private static FormLayoutElement createRoot(List<BaseFormElement> stepChildren) {
        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(new LinkedList<>(stepChildren));

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(new LinkedList<BaseStepElement>(List.of(step)));
        return root;
    }
}
