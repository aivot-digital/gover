package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeNotOperatorTest {

    @Test
    void evaluate() throws NoCodeException {
        var operator = new NoCodeNotOperator();
        var data = new ElementDerivationData(Map.of("a", true, "b", false));

        // Test true value
        assertEquals(NoCodeDataType.Boolean, operator.evaluate(data, true).getDataType());
        assertEquals(Boolean.FALSE, operator.evaluate(data, true).getValue());

        // Test false value
        assertEquals(NoCodeDataType.Boolean, operator.evaluate(data, false).getDataType());
        assertEquals(Boolean.TRUE, operator.evaluate(data, false).getValue());

        // Test null value
        assertThrows(NullPointerException.class, () -> operator.evaluate(data, null));

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, false));
    }
}