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
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.BaseStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementDerivationServiceV2Test {
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
                new NoCodeEvaluationService(List.of())
        );
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
