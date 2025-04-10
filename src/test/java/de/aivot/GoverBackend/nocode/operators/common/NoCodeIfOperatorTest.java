package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeIfOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeIfOperator();
        var data = new ElementDerivationData(Map.of("a", true, "b", false));

        // Test condition true
        assertEquals(NoCodeDataType.Any, operator.performEvaluation(data, true, "yes", "no").getDataType());
        assertEquals("yes", operator.performEvaluation(data, true, "yes", "no").getValue());

        // Test condition false
        assertEquals(NoCodeDataType.Any, operator.performEvaluation(data, false, "yes", "no").getDataType());
        assertEquals("no", operator.performEvaluation(data, false, "yes", "no").getValue());

        // Test null condition
        assertThrows(NoCodeException.class, () -> operator.performEvaluation(data, null, "yes", "no"));

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, true, "yes"));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, true, "yes", "no", "maybe"));
    }
}