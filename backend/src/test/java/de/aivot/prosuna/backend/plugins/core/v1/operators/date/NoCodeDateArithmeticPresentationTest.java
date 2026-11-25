package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeDateArithmeticPresentationTest {
    @ParameterizedTest
    @CsvSource({
            "add-to-date, #0 plus #1 #2, plus",
            "subtract-from-date, #0 minus #1 #2, minus"
    })
    void detailsShouldExposeReadableTemplatesAndUnitGrammarWithoutChangingStoredValues(
            String identifier,
            String template,
            String amountPhrase
    ) {
        var provider = new CommonOperatorsV1(
                null, null, new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())
        );
        var details = NoCodeOperatorDetailsDTO.fromSPI(provider)
                .filter(candidate -> candidate.identifier().equals(identifier))
                .findFirst()
                .orElseThrow();
        var signature = details.signatures()[0];
        var parameters = signature.parameters();

        assertEquals(template, details.humanReadableTemplate());
        assertEquals("ergibt", details.sentenceLead());
        assertEquals(NoCodeDataType.Date, signature.returnType());
        assertEquals("Ausgangsdatum", parameters[0].label());
        assertEquals("das Datum", parameters[0].sentencePhrase());
        assertEquals(amountPhrase, parameters[1].sentencePhrase());
        assertEquals("", parameters[2].sentencePhrase());

        var json = JsonMapperTestUtils.createMapper().valueToTree(details);
        var serializedParameters = json.get("signatures").get(0).get("parameters");
        assertEquals("Calendar", serializedParameters.get(0).get("dateInput").asString());
        var constraints = serializedParameters.get(1).get("numberConstraints");
        assertTrue(constraints.get("integerOnly").booleanValue());
        assertEquals((double) Integer.MIN_VALUE, constraints.get("minimum").doubleValue());
        assertEquals((double) Integer.MAX_VALUE, constraints.get("maximum").doubleValue());

        var unit = serializedParameters.get(2);
        assertEquals(1, unit.get("quantityParameterIndex").intValue());
        var expectedLabels = new String[]{"Tage", "Wochen", "Monate", "Jahre"};
        var expectedValues = new String[]{"tage", "wochen", "monate", "jahre"};
        var expectedSingulars = new String[]{"Tag", "Woche", "Monat", "Jahr"};
        for (var index = 0; index < expectedValues.length; index++) {
            var option = unit.get("options").get(index);
            assertEquals(expectedLabels[index], option.get("label").asString());
            assertEquals(expectedValues[index], option.get("value").asString());
            assertEquals(expectedSingulars[index], option.get("singularLabel").asString());
        }
    }

    @Test
    void pluginConstructorsShouldKeepTheirExistingDefaults() {
        var option = new NoCodeParameterOption("Bezeichnung", "value");
        var legacy = new NoCodeParameter(NoCodeDataType.String, "Wert", "Ein Text.", option);
        var withSentence = new NoCodeParameter(
                NoCodeDataType.String, "Wert", "Ein Text.", "wenn", "zutrifft", option
        );

        assertNull(option.singularLabel());
        assertNull(legacy.numberConstraints());
        assertNull(legacy.quantityParameterIndex());
        assertNull(withSentence.numberConstraints());
        assertNull(withSentence.quantityParameterIndex());
        assertEquals("wenn", withSentence.sentencePhrase());
        assertEquals("zutrifft", withSentence.sentenceSuffix());
        assertEquals(option, withSentence.options()[0]);
    }

    @ParameterizedTest
    @CsvSource({
            "add-to-date, 1, 2026-08-20",
            "subtract-from-date, -1, 2026-08-10"
    })
    void examplesAndNegativeOrZeroAmountsShouldMatchTheDescription(
            String identifier,
            int direction,
            String expectedExample
    ) throws NoCodeException {
        var operator = operator(identifier);
        var date = LocalDate.of(2026, 8, 15);

        assertEquals(LocalDate.parse(expectedExample), operator.evaluate(runtime(), date, 5, "tage").getValue());
        assertEquals(date, operator.evaluate(runtime(), date, 0, "tage").getValue());
        assertEquals(date.plusDays(-5L * direction), operator.evaluate(runtime(), date, -5, "tage").getValue());
        assertEquals(date.plusDays(5L * direction), operator.evaluate(runtime(), date, "5.0", "tage").getValue());
        assertEquals(date.plusDays(5L * direction), operator.evaluate(runtime(), date, "5e0", "tage").getValue());
    }

    @ParameterizedTest
    @CsvSource({
            "add-to-date, 1, 2026-01-31, 2025-02-28",
            "subtract-from-date, -1, 2026-03-31, 2023-02-28"
    })
    void monthEndAndLeapDayAdjustmentsShouldNotPromiseAnInverseCalculation(
            String identifier,
            int direction,
            String monthEnd,
            String leapResult
    ) throws NoCodeException {
        var operator = operator(identifier);
        var adjusted = operator.evaluate(runtime(), monthEnd, 1, "monate").getValue();

        assertEquals(LocalDate.of(2026, 2, 28), adjusted);
        assertEquals(LocalDate.parse(leapResult), operator.evaluate(runtime(), "2024-02-29", 1, "jahre").getValue());
        assertEquals(
                LocalDate.of(2026, direction == 1 ? 1 : 3, 28),
                operator.evaluate(runtime(), adjusted, -1, "monate").getValue()
        );
    }

    @ParameterizedTest
    @CsvSource({"add-to-date, 1", "subtract-from-date, -1"})
    void amountConstraintsShouldMatchTheEvaluatorIncludingTheNegativeBoundary(
            String identifier,
            int direction
    ) throws NoCodeException {
        var operator = operator(identifier);
        var date = LocalDate.of(2026, 8, 15);

        for (var amount : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            // The amount is accepted, but the result leaves the supported four-digit years.
            var error = assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), date, amount, "tage"));
            assertEquals("Der Datumswert liegt außerhalb des unterstützten Bereichs.", error.getMessage());
        }
        assertEquals(LocalDate.of(9999, 12, 31), operator.evaluate(runtime(), LocalDate.of(9999, 12, 30), direction, "tage").getValue());

        for (var invalidAmount : new String[]{"2147483648", "-2147483649", "5.5", "5,0", "keine Zahl"}) {
            assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), date, invalidAmount, "tage"));
        }
        assertThrows(
                NoCodeException.class,
                () -> operator.evaluate(runtime(), date, new BigDecimal("2147483647.0000000001"), "tage")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"add-to-date", "subtract-from-date"})
    void precisionErrorsShouldExplainTheAvailableUnits(String identifier) {
        var operator = operator(identifier);

        assertEquals(
                "Bei einem Datum mit Monat und Jahr können Sie nur Monate oder Jahre verwenden.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), "2026-08", 1, "tage")).getMessage()
        );
        assertEquals(
                "Bei einer Jahresangabe können Sie nur Jahre verwenden.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), "2026", 1, "monate")).getMessage()
        );
    }

    private NoCodeOperator operator(String identifier) {
        return identifier.equals("add-to-date") ? new NoCodeAddToDateOperator() : new NoCodeSubtractFromDateOperator();
    }
}
