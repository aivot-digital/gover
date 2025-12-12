package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.plugins.corePlugin.components.operators.common.NoCodeGreaterThanOrEqualOperator;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeGreaterThanOrEqualOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeGreaterThanOrEqualOperator();
        var data = ElementData.of("a", 5, "b", 3);

        // Test greater than
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 5, 3).getValue());

        // Test equal values
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 5, 5).getValue());

        // Test not greater than or equal
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 3, 5).getValue());

        // Test null values
        assertEquals(Boolean.TRUE, operator.evaluate(data, 5, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5, 3, 1));
    }
}