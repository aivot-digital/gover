package de.aivot.GoverBackend.plugins.core.v1.operators;

import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeIsInvisibleOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeConcatOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.date.NoCodeCreateTimeOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.list.NoCodeListContainsOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeSplitOperator;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static de.aivot.GoverBackend.TestData.runtime;
import static de.aivot.GoverBackend.TestData.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorHardeningTest {
    @Test
    void isInvisibleShouldInvertIsVisibleResult() throws NoCodeException {
        var operator = new NoCodeIsInvisibleOperator();
        var states = new ComputedElementStates();
        states.put("field-1", state(true));
        var data = runtime(new de.aivot.GoverBackend.elements.models.EffectiveElementValues(), states);

        var result = operator.performEvaluation(data, "field-1");

        assertFalse(result.getValueAsBoolean());
    }

    @Test
    void listContainsShouldHandleNullValuesCorrectly() throws NoCodeException {
        var operator = new NoCodeListContainsOperator();
        var data = runtime();

        assertTrue(operator.performEvaluation(data, Arrays.asList("a", null), null).getValueAsBoolean());
        assertFalse(operator.performEvaluation(data, List.of("a", "b"), null).getValueAsBoolean());
    }

    @Test
    void splitShouldTreatSeparatorAsLiteral() throws NoCodeException {
        var operator = new NoCodeSplitOperator();
        var data = runtime();

        var result = operator.performEvaluation(data, "a.b.c", ".");

        assertEquals(List.of("a", "b", "c"), result.getValue());
    }

    @Test
    void regexOperatorsShouldThrowNoCodeExceptionOnInvalidPattern() {
        var operator = new NoCodeRegexMatchOperator();
        var data = runtime();

        assertThrows(NoCodeException.class, () -> operator.performEvaluation(data, "abc", "["));
    }

    @Test
    void createTimeShouldNormalizeSecondsAndValidateRange() throws NoCodeException {
        var operator = new NoCodeCreateTimeOperator();
        var data = runtime();

        var validResult = operator.performEvaluation(data, 13, 45);
        var time = assertInstanceOf(ZonedDateTime.class, validResult.getValue());
        assertEquals(0, time.getSecond());
        assertEquals(0, time.getNano());

        assertThrows(NoCodeException.class, () -> operator.performEvaluation(data, 24, 0));
        assertThrows(NoCodeException.class, () -> operator.performEvaluation(data, 23, 60));
    }

    @Test
    void concatShouldConcatenateMultipleValues() throws NoCodeException {
        var operator = new NoCodeConcatOperator();
        var data = runtime();

        var result = operator.performEvaluation(data, "User ", 42, " active");

        assertEquals("User 42 active", result.getValue());
        assertThrows(NoCodeException.class, () -> operator.performEvaluation(data, "only-one"));
    }
}
