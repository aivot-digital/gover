package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.elements.enums.OverrideFunctionType;
import de.aivot.GoverBackend.elements.enums.ValidationFunctionType;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.enums.VisibilityFunctionType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementFunctionTypeInferenceTest {
    @Test
    void shouldInferValueFunctionTypeFromAssignedFunction() {
        assertEquals(
                ValueFunctionType.NoCode,
                new ElementValueFunctions()
                        .setNoCode(NoCodeStaticValue.of("value"))
                        .getType()
        );
        assertEquals(
                ValueFunctionType.Javascript,
                new ElementValueFunctions()
                        .setJavascriptCode(JavascriptCode.of("return 'value';"))
                        .getType()
        );
    }

    @Test
    void shouldInferVisibilityFunctionTypeFromAssignedFunction() {
        assertEquals(
                VisibilityFunctionType.NoCode,
                new ElementVisibilityFunctions()
                        .setNoCode(NoCodeStaticValue.of(true))
                        .getType()
        );
        assertEquals(
                VisibilityFunctionType.ConditionSet,
                new ElementVisibilityFunctions()
                        .setConditionSet(new ConditionSet())
                        .getType()
        );
        assertEquals(
                VisibilityFunctionType.Javascript,
                new ElementVisibilityFunctions()
                        .setJavascriptCode(JavascriptCode.of("return true;"))
                        .getType()
        );
    }

    @Test
    void shouldInferOverrideFunctionTypeFromAssignedFunction() {
        assertEquals(
                OverrideFunctionType.NoCode,
                new ElementOverrideFunctions()
                        .setFieldNoCodeMap(Map.of("label", NoCodeStaticValue.of("value")))
                        .getType()
        );
        assertEquals(
                OverrideFunctionType.Javascript,
                new ElementOverrideFunctions()
                        .setJavascriptCode(JavascriptCode.of("return element;"))
                        .getType()
        );
    }

    @Test
    void shouldInferValidationFunctionTypeFromAssignedFunction() {
        var validationWrapper = new ValidationNoCodeWrapper()
                .setNoCode(NoCodeStaticValue.of(true))
                .setMessage("valid");

        assertEquals(
                ValidationFunctionType.NoCode,
                new ElementValidationFunctions()
                        .setNoCodeList(List.of(validationWrapper))
                        .getType()
        );
        assertEquals(
                ValidationFunctionType.ConditionSet,
                new ElementValidationFunctions()
                        .setConditionSet(new ConditionSet())
                        .getType()
        );
        assertEquals(
                ValidationFunctionType.Javascript,
                new ElementValidationFunctions()
                        .setJavascriptCode(JavascriptCode.of("return null;"))
                        .getType()
        );
    }

    @Test
    void shouldNotOverrideExplicitlyAssignedType() {
        assertEquals(
                ValueFunctionType.Javascript,
                new ElementValueFunctions()
                        .setType(ValueFunctionType.Javascript)
                        .setNoCode(NoCodeStaticValue.of("value"))
                        .getType()
        );
        assertEquals(
                VisibilityFunctionType.Javascript,
                new ElementVisibilityFunctions()
                        .setType(VisibilityFunctionType.Javascript)
                        .setNoCode(NoCodeStaticValue.of(true))
                        .getType()
        );
        assertEquals(
                OverrideFunctionType.Javascript,
                new ElementOverrideFunctions()
                        .setType(OverrideFunctionType.Javascript)
                        .setFieldNoCodeMap(Map.of("label", NoCodeStaticValue.of("value")))
                        .getType()
        );
        assertEquals(
                ValidationFunctionType.Javascript,
                new ElementValidationFunctions()
                        .setType(ValidationFunctionType.Javascript)
                        .setNoCodeList(List.of(new ValidationNoCodeWrapper().setNoCode(NoCodeStaticValue.of(true))))
                        .getType()
        );
    }
}
