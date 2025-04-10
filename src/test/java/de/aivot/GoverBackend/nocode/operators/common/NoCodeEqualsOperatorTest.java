package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeEqualsOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeEqualsOperator();
        var data = new ElementDerivationData(Map.of("a", "a", "b", "b"));

        // Test equal values
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, "a", "a").getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, "a", "a").getValue());

        // Test different values
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, "a", "b").getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, "a", "b").getValue());

        // Test null values
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, null, null).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, null, null).getValue());

        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, "a", null).getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, "a", null).getValue());

        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, null, "a").getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, "a").getValue());

        // Test casting and comparison
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, 123, "123").getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 123, "123").getValue());

        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, 123, "abc").getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 123, "abc").getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, "a"));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, "a", "b", "c"));
    }
}