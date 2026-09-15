package de.aivot.prosuna.backend.plugins.core.v1.operators.phone;

import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import org.junit.jupiter.api.Test;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodePhoneNumberOperatorsTest {
    @Test
    void validOperatorShouldStrictlyValidatePhoneNumbers() throws NoCodeException {
        var operator = new NoCodePhoneNumberIsValidOperator();
        var data = runtime();

        assertTrue(operator.evaluate(data, "+49 30 123456").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "+49 1234").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "030 123456").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "+49 30 123456 ext. 7").getValueAsBoolean());
        assertFalse(operator.evaluate(data, (Object) null).getValueAsBoolean());
    }

    @Test
    void possibleOperatorShouldAcceptPlausiblePhoneNumbers() throws NoCodeException {
        var operator = new NoCodePhoneNumberIsPossibleOperator();
        var data = runtime();

        assertTrue(operator.evaluate(data, "+49 30 123456").getValueAsBoolean());
        assertTrue(operator.evaluate(data, "+49 1234").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "030 123456").getValueAsBoolean());
        assertFalse(operator.evaluate(data, "+49 30 123456 ext. 7").getValueAsBoolean());
        assertFalse(operator.evaluate(data, (Object) null).getValueAsBoolean());
    }

    @Test
    void normalizeOperatorShouldNormalizePossiblePhoneNumbersToE164() throws NoCodeException {
        var operator = new NoCodePhoneNumberNormalizeOperator();
        var data = runtime();

        assertEquals("+4930123456", operator.evaluate(data, "+49 30 123456").getValue());
        assertEquals("+491234", operator.evaluate(data, "+49 1234").getValue());
        assertNull(operator.evaluate(data, "030 123456").getValue());
        assertNull(operator.evaluate(data, "+49 30 123456 ext. 7").getValue());
        assertNull(operator.evaluate(data, (Object) null).getValue());
    }

    @Test
    void operatorsShouldRejectWrongArgumentCount() {
        var data = runtime();

        assertThrows(NoCodeException.class, () -> new NoCodePhoneNumberIsValidOperator().evaluate(data));
        assertThrows(NoCodeException.class, () -> new NoCodePhoneNumberIsPossibleOperator().evaluate(data, "+49 30 123456", "+49 40 123456"));
        assertThrows(NoCodeException.class, () -> new NoCodePhoneNumberNormalizeOperator().evaluate(data));
    }
}
