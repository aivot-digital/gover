package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.plugins.corePlugin.components.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeNotOperatorTest {

    @Test
    void evaluate() throws NoCodeException {
        var operator = new NoCodeNotOperator();
        var data = ElementData.of("a", true, "b", false);

        // Test true value
        assertEquals(Boolean.FALSE, operator.evaluate(data, true).getValue());

        // Test false value
        assertEquals(Boolean.TRUE, operator.evaluate(data, false).getValue());

        // Test null value
        assertThrows(NullPointerException.class, () -> operator.evaluate(data, null));

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, false));
    }
}