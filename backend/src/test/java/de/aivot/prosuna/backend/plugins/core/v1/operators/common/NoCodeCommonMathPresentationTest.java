package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeDivideOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeRoundDownOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeRoundUpOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeCommonMathPresentationTest {
    @ParameterizedTest
    @CsvSource({
            "2.344, 2.34, 2.34",
            "2.345, 2.35, 2.34",
            "2.346, 2.35, 2.35",
            "-2.344, -2.34, -2.34",
            "-2.345, -2.35, -2.34",
            "-2.346, -2.35, -2.35"
    })
    void roundingShouldChooseNearestValueAndDistinguishMidpoints(
            String input,
            String midpointAway,
            String midpointToward
    ) throws NoCodeException {
        var away = new NoCodeRoundUpOperator();
        var toward = new NoCodeRoundDownOperator();

        assertEquals(new BigDecimal(midpointAway), away.evaluate(runtime(), input, 2).getValue());
        assertEquals(new BigDecimal(midpointToward), toward.evaluate(runtime(), input, 2).getValue());
        assertTrue(away.getLabel().contains("von null weg"));
        assertTrue(toward.getLabel().contains("zu null hin"));
    }

    @Test
    void roundingShouldSupportWholeNumbersAndNegativePlacesAsDescribed() throws NoCodeException {
        assertEquals(new BigDecimal("3"), new NoCodeRoundUpOperator().evaluate(runtime(), "2.5", 0).getValue());
        assertEquals(new BigDecimal("2"), new NoCodeRoundDownOperator().evaluate(runtime(), "2.5", 0).getValue());
        assertEquals(new BigDecimal("1.3E+2"), new NoCodeRoundUpOperator().evaluate(runtime(), 125, -1).getValue());
        assertEquals(new BigDecimal("1.2E+2"), new NoCodeRoundDownOperator().evaluate(runtime(), 125, -1).getValue());
        for (var operator : List.of(new NoCodeRoundUpOperator(), new NoCodeRoundDownOperator())) {
            assertEquals(new BigDecimal("0E+4"), operator.evaluate(runtime(), "-12.3", -4).getValue());
        }
    }

    @Test
    void roundingRequiresAnExactlyRepresentableIntegerForDecimalPlaces() {
        for (var operator : List.of(new NoCodeRoundUpOperator(), new NoCodeRoundDownOperator())) {
            var constraints = operator.getSignatures()[0].parameters()[1].numberConstraints();
            assertTrue(constraints.integerOnly());
            assertEquals((double) Integer.MIN_VALUE, constraints.minimum());
            assertEquals((double) Integer.MAX_VALUE, constraints.maximum());
            for (var invalid : List.of("2.000000001", "2147483648", "-2147483649", "abc")) {
                var error = assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 2.345, invalid));
                assertTrue(error.getMessage().contains("Nachkommastellen"));
            }
        }
    }

    @Test
    void divisionShouldRoundNonTerminatingResultsToEightDecimalPlaces() throws NoCodeException {
        assertEquals(new BigDecimal("0.33333333"), new NoCodeDivideOperator().evaluate(runtime(), 1, 3).getValue());
        assertEquals(new BigDecimal("0.16666667"), new NoCodeDivideOperator().evaluate(runtime(), 1, 3, 2).getValue());
    }

    @Test
    void emptinessShouldTreatFalseAndZeroAsValuesAndCheckOnlyOuterStructures() throws NoCodeException {
        var defined = new NoCodeIsDefinedOperator();
        var undefined = new NoCodeIsUndefinedOperator();

        for (var value : List.of(false, 0, List.of(""), Map.of("value", ""), "[]")) {
            assertTrue(defined.evaluate(runtime(), value).getValueAsBoolean());
            assertFalse(undefined.evaluate(runtime(), value).getValueAsBoolean());
        }
        for (var value : List.of("\t\n", List.of(), Map.of())) {
            assertFalse(defined.evaluate(runtime(), value).getValueAsBoolean());
            assertTrue(undefined.evaluate(runtime(), value).getValueAsBoolean());
        }
    }

    @Test
    void valueShouldReturnTheArgumentWithoutLookingUpAnElement() throws NoCodeException {
        var operator = new NoCodeValueOperator();
        var object = Map.of("ort", "Berlin");

        assertEquals(NoCodeDataType.Runtime, operator.getSignatures()[0].parameters()[0].type());
        assertEquals("email", operator.evaluate(runtime("email", "person@example.org"), "email").getValue());
        assertSame(object, operator.evaluate(runtime(), object).getValue());
        assertEquals("#0", operator.getHumanReadableTemplate());
    }

    @Test
    void objectPropertyShouldUseExactTopLevelKeysAndReturnMissingAsNull() throws NoCodeException {
        var operator = new NoCodeObjectGetOperator();
        var object = Map.of("ort", "Berlin", "adresse.ort", "Bonn", "adresse", Map.of("ort", "Köln"));

        assertEquals("Berlin", operator.evaluate(runtime(), object, "ort").getValue());
        assertEquals("Bonn", operator.evaluate(runtime(), object, "adresse.ort").getValue());
        assertNull(operator.evaluate(runtime(), object, "Ort").getValue());
        assertEquals("Berlin", operator.evaluate(runtime(), "{\"ort\":\"Berlin\"}", "ort").getValue());
        assertNull(operator.evaluate(runtime(), null, "ort").getValue());
        assertNull(operator.evaluate(runtime(), "null", "ort").getValue());
        assertNull(operator.evaluate(runtime(), "{}", "ort").getValue());
        for (var invalid : List.of("kein JSON", "{} {}", "[]", 42, List.of("Berlin"))) {
            var error = assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), invalid, "ort"));
            assertTrue(error.getMessage().contains("Objekt"));
        }
    }

    @Test
    void numericComparisonsShouldHaveCompleteDistinctSentences() {
        var operators = List.of(
                new NoCodeGreaterThanOperator(), new NoCodeGreaterThanOrEqualOperator(),
                new NoCodeLessThanOperator(), new NoCodeLessThanOrEqualOperator()
        );
        for (var operator : operators) {
            var comparison = operator.getSignatures()[0].parameters()[1];
            assertEquals("ist", comparison.sentenceSuffix());
            assertEquals("#0 ist " + comparison.sentencePhrase() + " #1", operator.getHumanReadableTemplate());
        }
    }
}
