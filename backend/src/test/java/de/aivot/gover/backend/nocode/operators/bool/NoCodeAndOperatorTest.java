package de.aivot.gover.backend.nocode.operators.bool;

import de.aivot.gover.backend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;

import static de.aivot.gover.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.*;

class NoCodeAndOperatorTest {

    @Test
    void performEvaluation() throws NoCodeException {
        var operator = new NoCodeAndOperator();
        var data = runtime("a", true, "b", false);

        // Test both true
        assertEquals(Boolean.TRUE, operator.performEvaluation(data, true, true).getValue());

        // Test one true, one false
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, true, false).getValue());

        // Test both false
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, false, false).getValue());

        // Test null values
        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, true).getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, true, null).getValue());

        assertEquals(Boolean.FALSE, operator.performEvaluation(data, null, null).getValue());

        // Test wrong argument count
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(data, true, false, true));
    }
}
