package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.core.operators.common.NoCodeIfOperator;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeIfOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeIfOperator();
        var data = ElementData.of("a", true, "b", false);

        // Test condition true
        assertEquals("yes", operator.performEvaluation(data, true, "yes", "no").getValue());

        // Test condition false
        assertEquals("no", operator.performEvaluation(data, false, "yes", "no").getValue());

        // Test null condition
        assertEquals("no", operator.evaluate(data, null, "yes", "no").getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, "yes"));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, "yes", "no", "maybe"));
    }
}