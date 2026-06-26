package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeGreaterThanOperator;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeGreaterThanOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeGreaterThanOperator();
        var data = runtime("a", 5, "b", 3);

        // Test greater than
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 5, 3).getValue());

        // Test not greater than
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 3, 5).getValue());

        // Test equal values
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 5, 5).getValue());

        // Test null values
        assertEquals(Boolean.TRUE, operator.evaluate(data, 5, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5, 3, 1));
    }
}
