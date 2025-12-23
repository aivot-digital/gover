package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeOrOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeOrOperator();
        var data = ElementData.of("a", true, "b", false);

        // Test both true
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, true).getValue());

        // Test one true, one false
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, false).getValue());

        // Test both false
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, false, false).getValue());

        // Test null values
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, null, true).getValue());

        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, null).getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, false, true));
    }
}