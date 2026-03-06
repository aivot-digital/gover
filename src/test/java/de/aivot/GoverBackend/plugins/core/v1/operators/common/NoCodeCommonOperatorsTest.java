package de.aivot.GoverBackend.plugins.core.v1.operators.common;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeCommonOperatorsTest {
    @Test
    void isVisibleShouldResolveVisibilityFromElementData() throws NoCodeException {
        var data = new ElementData();
        data.put("visible", new ElementDataObject().setIsVisible(true));
        data.put("hidden", new ElementDataObject().setIsVisible(false));

        var operator = new NoCodeIsVisibleOperator();

        assertTrue(operator.evaluate(data, "visible").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "hidden").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "missing").getValueAsBoolean());
    }

    @Test
    void isInvisibleShouldBeInverseOfIsVisible() throws NoCodeException {
        var data = new ElementData();
        data.put("visible", new ElementDataObject().setIsVisible(true));
        data.put("hidden", new ElementDataObject().setIsVisible(false));

        var operator = new NoCodeIsInvisibleOperator();

        assertFalse(operator.evaluate(data, "visible").getValueAsBoolean());
        assertTrue(operator.evaluate(data, "hidden").getValueAsBoolean());
        assertTrue(operator.evaluate(data, "missing").getValueAsBoolean());
    }

    @Test
    void greaterThanOperatorsShouldCompareNumericValues() throws NoCodeException {
        var data = new ElementData();

        assertTrue(new NoCodeGreaterThanOperator().evaluate(data, 10, 5).getValueAsBoolean());
        assertFalse(new NoCodeGreaterThanOperator().evaluate(data, 5, 10).getValueAsBoolean());
        assertTrue(new NoCodeGreaterThanOrEqualOperator().evaluate(data, 10, 10).getValueAsBoolean());
        assertFalse(new NoCodeGreaterThanOrEqualOperator().evaluate(data, 5, 10).getValueAsBoolean());
    }

    @Test
    void lessThanOperatorsShouldCompareNumericValues() throws NoCodeException {
        var data = new ElementData();

        assertTrue(new NoCodeLessThanOperator().evaluate(data, 5, 10).getValueAsBoolean());
        assertFalse(new NoCodeLessThanOperator().evaluate(data, 10, 5).getValueAsBoolean());
        assertTrue(new NoCodeLessThanOrEqualOperator().evaluate(data, 10, 10).getValueAsBoolean());
        assertFalse(new NoCodeLessThanOrEqualOperator().evaluate(data, 15, 10).getValueAsBoolean());
    }

    @Test
    void equalsAndNotEqualsShouldHandleNullAndTypeCasting() throws NoCodeException {
        var data = new ElementData();
        var equals = new NoCodeEqualsOperator();
        var notEquals = new NoCodeNotEqualsOperator();

        assertTrue(equals.evaluate(data, null, null).getValueAsBoolean());
        assertFalse(equals.evaluate(data, null, "x").getValueAsBoolean());
        assertTrue(equals.evaluate(data, 5, "5").getValueAsBoolean());

        assertFalse(notEquals.evaluate(data, 5, "5").getValueAsBoolean());
        assertTrue(notEquals.evaluate(data, 5, "6").getValueAsBoolean());
    }

    @Test
    void isDefinedShouldTreatBlankAndEmptyStructuresAsUndefined() throws NoCodeException {
        var data = new ElementData();
        var operator = new NoCodeIsDefinedOperator();

        assertFalse(operator.evaluate(data, (Object) null).getValueAsBoolean());
        assertFalse(operator.evaluate(data, "").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "   ").getValueAsBoolean());
        assertFalse(operator.evaluate(data, List.of()).getValueAsBoolean());
        assertFalse(operator.evaluate(data, Map.of()).getValueAsBoolean());
        assertTrue(operator.evaluate(data, "abc").getValueAsBoolean());
        assertTrue(operator.evaluate(data, 0).getValueAsBoolean());
    }

    @Test
    void isUndefinedShouldBeInverseOfIsDefined() throws NoCodeException {
        var data = new ElementData();
        var operator = new NoCodeIsUndefinedOperator();

        assertTrue(operator.evaluate(data, (Object) null).getValueAsBoolean());
        assertTrue(operator.evaluate(data, "").getValueAsBoolean());
        assertTrue(operator.evaluate(data, List.of()).getValueAsBoolean());
        assertFalse(operator.evaluate(data, "abc").getValueAsBoolean());
    }

    @Test
    void ifShouldReturnThenOrElseBranchBasedOnTruthyValue() throws NoCodeException {
        var data = new ElementData();
        var operator = new NoCodeIfOperator();

        assertEquals("then", operator.evaluate(data, true, "then", "else").getValue());
        assertEquals("else", operator.evaluate(data, "false", "then", "else").getValue());
        assertEquals("then", operator.evaluate(data, 1, "then", "else").getValue());
    }

    @Test
    void valueShouldReturnProvidedArgumentUnchanged() throws NoCodeException {
        var data = new ElementData();
        var operator = new NoCodeValueOperator();

        assertEquals("abc", operator.evaluate(data, "abc").getValue());
        assertEquals(42, operator.evaluate(data, 42).getValue());
    }

    @Test
    void valueShouldRejectWrongArgumentCount() {
        var data = new ElementData();
        var operator = new NoCodeValueOperator();

        assertThrows(NoCodeException.class, () -> operator.evaluate(data));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, "a", "b"));
    }
}
