package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeOrOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeOrOperator();
        var data = new ElementDerivationData(Map.of("a", true, "b", false));

        // Test both true
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, true, true).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, true).getValue());

        // Test one true, one false
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, true, false).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, false).getValue());

        // Test both false
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, false, false).getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, false, false).getValue());

        // Test null values
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, null, true).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, null, true).getValue());

        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, true, null).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, null).getValue());

        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, null, null).getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, true));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.performEvaluation(data, true, false, true));
    }
}