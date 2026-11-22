package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeDateTimeArithmeticPresentationTest {
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Berlin");

    private ZoneId originalZone;

    @BeforeEach
    void setUp() {
        originalZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(TEST_ZONE);
    }

    @AfterEach
    void tearDown() {
        ApplicationTimeZone.configure(originalZone);
    }

    @ParameterizedTest
    @CsvSource({
            "add-to-datetime, Zum Zeitpunkt hinzufügen, #0 plus #1 #2, plus",
            "subtract-from-datetime, Vom Zeitpunkt abziehen, #0 minus #1 #2, minus"
    })
    void operatorDetailsShouldExposeReadableDateTimeArithmeticAndAuthoringConstraints(
            String identifier,
            String label,
            String template,
            String amountPhrase
    ) {
        var provider = new CommonOperatorsV1(null, null, new BusinessTime(TEST_ZONE, Clock.systemUTC()));
        var details = NoCodeOperatorDetailsDTO.fromSPI(provider)
                .filter(candidate -> candidate.identifier().equals(identifier))
                .findFirst()
                .orElseThrow();
        var signature = details.signatures()[0];
        var parameters = signature.parameters();

        assertEquals(label, details.label());
        assertEquals(template, details.humanReadableTemplate());
        assertEquals("ergibt", details.sentenceLead());
        assertEquals(NoCodeDataType.DateTime, signature.returnType());
        assertEquals("Ausgangszeitpunkt", parameters[0].label());
        assertEquals(NoCodeDataType.DateTime, parameters[0].type());
        assertEquals("den Zeitpunkt", parameters[0].sentencePhrase());
        assertEquals(amountPhrase, parameters[1].sentencePhrase());
        assertEquals("", parameters[2].sentencePhrase());

        var serialized = JsonMapperTestUtils.createMapper().valueToTree(details);
        var serializedParameters = serialized.get("signatures").get(0).get("parameters");
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

    @ParameterizedTest
    @EnumSource(NoCodeAdjustDateTimeOperator.Direction.class)
    void examplesAndSignedAmountsShouldKeepTheOriginalPrecision(NoCodeAdjustDateTimeOperator.Direction direction) throws NoCodeException {
        var operator = new NoCodeAdjustDateTimeOperator(direction);
        var sign = direction == NoCodeAdjustDateTimeOperator.Direction.ADD ? 1 : -1;
        var source = Instant.parse("2026-11-22T12:30:00.123456789Z");

        assertEquals(source.plus(Duration.ofDays(5L * sign)), operator.evaluate(runtime(), source, 5, "tage").getValue());
        assertEquals(source.minus(Duration.ofDays(5L * sign)), operator.evaluate(runtime(), source, -5, "tage").getValue());
        assertEquals(source, operator.evaluate(runtime(), source, 0, "tage").getValue());
        assertEquals(source.plus(Duration.ofDays(5L * sign)), operator.evaluate(runtime(), source, "5.0", "tage").getValue());
        assertEquals(source.plus(Duration.ofDays(10L * sign)), operator.evaluate(runtime(), source, "1e1", "tage").getValue());
    }

    @ParameterizedTest
    @CsvSource({
            "ADD, 2026-03-28T02:30:00+01:00, 2026-03-29T03:30:00+02:00",
            "SUBTRACT, 2026-03-30T02:30:00+02:00, 2026-03-29T03:30:00+02:00",
            "ADD, 2026-10-24T02:30:00+02:00, 2026-10-25T02:30:00+02:00",
            "SUBTRACT, 2026-10-26T02:30:00+01:00, 2026-10-25T02:30:00+01:00"
    })
    void calendarDaysShouldResolveSkippedAndRepeatedTargetTimesAsDescribed(
            NoCodeAdjustDateTimeOperator.Direction direction,
            String source,
            String expected
    ) throws NoCodeException {
        var operator = new NoCodeAdjustDateTimeOperator(direction);

        assertEquals(OffsetDateTime.parse(expected).toInstant(), operator.evaluate(runtime(), source, 1, "tage").getValue());
    }

    @ParameterizedTest
    @CsvSource({
            "2026-03-28T12:00:00+01:00, 2026-03-29T12:00:00+02:00, 23",
            "2026-10-24T12:00:00+02:00, 2026-10-25T12:00:00+01:00, 25"
    })
    void aCalendarDayShouldNotPromiseTwentyFourElapsedHours(String source, String expected, long hours) throws NoCodeException {
        var operator = new NoCodeAdjustDateTimeOperator(NoCodeAdjustDateTimeOperator.Direction.ADD);
        var actual = (Instant) operator.evaluate(runtime(), source, 1, "tage").getValue();

        assertEquals(OffsetDateTime.parse(expected).toInstant(), actual);
        assertEquals(hours, Duration.between(OffsetDateTime.parse(source).toInstant(), actual).toHours());
    }

    @ParameterizedTest
    @CsvSource({
            "ADD, 2026-01-31T09:00:00+01:00, monate, 2026-02-28T09:00:00+01:00",
            "SUBTRACT, 2026-03-31T09:00:00+02:00, monate, 2026-02-28T09:00:00+01:00",
            "ADD, 2024-02-29T09:00:00+01:00, jahre, 2025-02-28T09:00:00+01:00",
            "SUBTRACT, 2024-02-29T09:00:00+01:00, jahre, 2023-02-28T09:00:00+01:00"
    })
    void calendarAdjustmentsShouldUseTheLastExistingDay(
            NoCodeAdjustDateTimeOperator.Direction direction,
            String source,
            String unit,
            String expected
    ) throws NoCodeException {
        var operator = new NoCodeAdjustDateTimeOperator(direction);

        assertEquals(OffsetDateTime.parse(expected).toInstant(), operator.evaluate(runtime(), source, 1, unit).getValue());
    }

    @ParameterizedTest
    @EnumSource(NoCodeAdjustDateTimeOperator.Direction.class)
    void invalidInputsAndOutOfRangeResultsShouldExplainTheCorrection(NoCodeAdjustDateTimeOperator.Direction direction) {
        var operator = new NoCodeAdjustDateTimeOperator(direction);
        var source = "2026-11-22T13:30:00+01:00";

        for (var invalidSource : new Object[]{null, "kein Zeitpunkt", "2026-11-22"}) {
            assertEquals(
                    "Der Ausgangszeitpunkt ist ungültig. Verwenden Sie einen Wert mit Datum und Uhrzeit.",
                    assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), invalidSource, 1, "tage")).getMessage()
            );
        }
        assertEquals(
                "Die Einheit ist ungültig. Verwenden Sie Tage, Wochen, Monate oder Jahre.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), source, 1, "stunden")).getMessage()
        );
        assertEquals(
                "Der berechnete Zeitpunkt liegt außerhalb des unterstützten Bereichs. Prüfen Sie Ausgangszeitpunkt und Anzahl.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), source, Integer.MAX_VALUE, "jahre")).getMessage()
        );
        for (var invalidAmount : new String[]{"1.5", "1,0", "2147483648", "-2147483649"}) {
            assertEquals(
                    "Die Anzahl muss eine ganze Zahl sein.",
                    assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), source, invalidAmount, "tage")).getMessage()
            );
        }
    }
}
