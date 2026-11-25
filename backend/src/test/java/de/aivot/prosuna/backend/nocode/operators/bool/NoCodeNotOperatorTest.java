package de.aivot.prosuna.backend.nocode.operators.bool;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoCodeNotOperatorTest {

    @Test
    void invertsBooleanValuesAndTheirFixedSelectionValues() throws NoCodeException {
        var operator = new NoCodeNotOperator();
        var data = runtime();

        assertEquals(Boolean.FALSE, operator.evaluate(data, true).getValue());
        assertEquals(Boolean.TRUE, operator.evaluate(data, false).getValue());
        assertEquals(Boolean.FALSE, operator.evaluate(data, "true").getValue());
        assertEquals(Boolean.TRUE, operator.evaluate(data, "false").getValue());
    }

    @Test
    void treatsAMissingOperandValueAsFalseBeforeNegation() throws NoCodeException {
        var operator = new NoCodeNotOperator();

        assertEquals(Boolean.TRUE, operator.evaluate(runtime(), (Object) null).getValue());
    }

    @Test
    void rejectsAMissingParameterArray() {
        var operator = new NoCodeNotOperator();

        assertThrows(NullPointerException.class, () -> operator.evaluate(runtime(), (Object[]) null));
    }

    @Test
    void requiresExactlyOneOperand() {
        var operator = new NoCodeNotOperator();

        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(runtime()));
        assertThrows(NoCodeWrongArgumentCountException.class, () -> operator.evaluate(runtime(), true, false));
    }

    @ParameterizedTest
    @CsvSource({
            "Berlin, false, true",
            "Hamburg, true, false"
    })
    void negatesNestedEqualityAndInequality(String birthplace, boolean negatedEquality, boolean negatedInequality) {
        var service = new NoCodeEvaluationService(List.of(new CommonOperatorsV1(
                null,
                null,
                new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())
        )));
        var data = runtime("birthplace", birthplace);
        var equal = NoCodeExpression.of("equals", NoCodeReference.of("birthplace"), NoCodeStaticValue.of("Berlin"));
        var notEqual = NoCodeExpression.of("not-equals", NoCodeReference.of("birthplace"), NoCodeStaticValue.of("Berlin"));

        assertEquals(negatedEquality, service.evaluate(NoCodeExpression.of("not", equal), data).getValueAsBoolean());
        assertEquals(negatedInequality, service.evaluate(NoCodeExpression.of("not", notEqual), data).getValueAsBoolean());
    }
}
