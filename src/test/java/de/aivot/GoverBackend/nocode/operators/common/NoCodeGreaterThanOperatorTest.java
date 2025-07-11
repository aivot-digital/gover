package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.core.operators.common.NoCodeGreaterThanOperator;
import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NoCodeGreaterThanOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeGreaterThanOperator();
        var data = new ElementDerivationData(Map.of("a", 5, "b", 3));

        // Test greater than
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, 5, 3).getDataType());
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, 5, 3).getValue());

        // Test not greater than
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, 3, 5).getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 3, 5).getValue());

        // Test equal values
        assertEquals(NoCodeDataType.Boolean, operator.performEvaluation(data, 5, 5).getDataType());
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, 5, 5).getValue());

        // Test null values
        assertEquals(NoCodeDataType.Boolean, operator.evaluate(data, null, 5).getDataType());
        assertEquals(Boolean.TRUE, operator.evaluate(data, 5, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, 5, 3, 1));
    }
}