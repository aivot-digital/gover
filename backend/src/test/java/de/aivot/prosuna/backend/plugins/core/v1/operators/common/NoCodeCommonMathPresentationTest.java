package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeAddOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeDivideOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeMultiplyOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeRoundDownOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeRoundUpOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeSubtractOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertEquals(new BigDecimal("130"), new NoCodeRoundUpOperator().evaluate(runtime(), 125, -1).getValue());
        assertEquals(new BigDecimal("120"), new NoCodeRoundDownOperator().evaluate(runtime(), 125, -1).getValue());
        for (var operator : List.of(new NoCodeRoundUpOperator(), new NoCodeRoundDownOperator())) {
            assertEquals(BigDecimal.ZERO, operator.evaluate(runtime(), "-12.3", -4).getValue());
        }
    }

    @Test
    void roundingRequiresAnExactlyRepresentableIntegerForDecimalPlaces() throws NoCodeException {
        for (var operator : List.of(new NoCodeRoundUpOperator(), new NoCodeRoundDownOperator())) {
            var constraints = operator.getSignatures()[0].parameters()[1].numberConstraints();
            assertTrue(constraints.integerOnly());
            assertEquals(-15.0, constraints.minimum());
            assertEquals(15.0, constraints.maximum());
            assertEquals(new BigDecimal("2300"), operator.evaluate(runtime(), 2345, -2).getValue());
            for (var invalid : List.of("2.000000001", "2147483648", "-2147483649", "abc", "16", "-16")) {
                var error = assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), 2.345, invalid));
                assertTrue(error.getMessage().contains("Nachkommastellen"));
            }
        }
    }

    @Test
    void divisionShouldRoundNonTerminatingResultsToEightDecimalPlaces() throws NoCodeException {
        assertEquals(new BigDecimal("0.33333333"), new NoCodeDivideOperator().evaluate(runtime(), 1, 3).getValue());
        assertEquals(new BigDecimal("0.16666667"), new NoCodeDivideOperator().evaluate(runtime(), 1, 3, 2).getValue());
        assertEquals(new BigDecimal("3.33333330"), new NoCodeDivideOperator().evaluate(runtime(), 1, 3, "0.1").getValue());
        assertEquals(new BigDecimal("3.33333333"), new NoCodeDivideOperator().evaluate(runtime(), 1, "0.1", 3).getValue());
        assertThrows(NoCodeException.class, () -> new NoCodeDivideOperator().evaluate(runtime(), 24, 3, 0));
    }

    @Test
    void subtractionShouldUseTheFirstOperandAsTheStartingNumber() throws NoCodeException {
        var operator = new NoCodeSubtractOperator();
        assertEquals(new BigDecimal("12"), operator.evaluate(runtime(), 20, 5, 3).getValue());
        assertEquals(new BigDecimal("-18"), operator.evaluate(runtime(), 5, 20, 3).getValue());
    }

    static Stream<Arguments> arithmeticExamples() {
        return Stream.of(
                Arguments.of(new NoCodeAddOperator(), "Zahl 2", "Zahl 3", 5, 3, 2, 10),
                Arguments.of(new NoCodeMultiplyOperator(), "Zahl 2", "Zahl 3", 5, 3, 2, 30),
                Arguments.of(new NoCodeSubtractOperator(), "Abzuziehende Zahl 1", "Abzuziehende Zahl 2", 20, 5, 3, 12),
                Arguments.of(new NoCodeDivideOperator(), "Teiler 1", "Teiler 2", 24, 3, 2, 4)
        );
    }

    @ParameterizedTest
    @MethodSource("arithmeticExamples")
    void arithmeticExamplesShouldDescribeAndEvaluateTheAdditionalOperand(
            NoCodeOperator operator, String repeatedLabel, String thirdLabel, int first, int second, int third, int expected
    ) throws NoCodeException {
        var parameters = operator.getSignatures()[0].parameters();
        assertTrue(operator.hasVariableArgumentCount());
        assertEquals(2, parameters.length);
        assertEquals(repeatedLabel, parameters[1].label());
        var phrase = parameters[1].sentencePhrase();
        var expression = first + " " + phrase + " " + second + " " + phrase + " " + third;
        assertTrue(operator.getDescription().contains("„Weitere Zahl hinzufügen“"));
        assertTrue(operator.getDescription().contains("„" + thirdLabel + "“"));
        assertTrue(operator.getDescription().contains("`" + expression + "` ergibt `" + expected + "`"));
        var result = (BigDecimal) operator.evaluate(runtime(), first, second, third).getValue();
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(result));
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
