package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeEqualsOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeEqualsOperator();
        var data = runtime("a", "a", "b", "b");

        // Test equal values
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, "a", "a").getValue());

        // Test different values
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, "a", "b").getValue());

        // Test null values
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, null, null).getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, "a", null).getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, "a").getValue());

        // Test casting and comparison
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 123, "123").getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 123, "abc").getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, "a"));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, "a", "b", "c"));
    }
}
