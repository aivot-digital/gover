package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeTemporalComparePresentationTest {
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Berlin");
    private static final String[] OPERATIONS = {"gleich", "ungleich", "vor", "vor-oder-gleich", "nach", "nach-oder-gleich"};
    private static final String[] OPTION_LABELS = {"gleich", "nicht gleich", "früher als", "früher als oder gleich", "später als", "später als oder gleich"};

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
    @EnumSource(NoCodeTemporalCompareOperator.TemporalType.class)
    void detailsShouldDeclareFixedSentencePartsAndTypedComparisonInputs(NoCodeTemporalCompareOperator.TemporalType type) {
        var operator = new NoCodeTemporalCompareOperator(type);
        var details = NoCodeOperatorDetailsDTO.fromSPI(provider())
                .filter(candidate -> candidate.identifier().equals(operator.getIdentifier()))
                .findFirst()
                .orElseThrow();
        var signature = details.signatures()[0];
        var parameters = signature.parameters();
        var expectedType = switch (type) {
            case DATE -> NoCodeDataType.Date;
            case TIME -> NoCodeDataType.Time;
            case DATETIME -> NoCodeDataType.DateTime;
        };
        var expectedLabels = switch (type) {
            case DATE -> new String[]{"Datum", "Vergleichsart", "Vergleichsdatum"};
            case TIME -> new String[]{"Uhrzeit", "Vergleichsart", "Vergleichsuhrzeit"};
            case DATETIME -> new String[]{"Zeitpunkt", "Vergleichsart", "Vergleichszeitpunkt"};
        };

        assertEquals(NoCodeDataType.Boolean, signature.returnType());
        assertEquals("#0 ist #1 #2", details.humanReadableTemplate());
        assertEquals("ergibt wahr", details.sentenceLead());
        assertEquals(3, parameters.length);
        assertEquals(expectedType, parameters[0].type());
        assertEquals(NoCodeDataType.String, parameters[1].type());
        assertEquals(expectedType, parameters[2].type());
        assertEquals("wenn", parameters[0].sentencePhrase());
        assertEquals("", parameters[1].sentencePhrase());
        assertEquals("", parameters[2].sentencePhrase());
        assertEquals("ist", parameters[2].sentenceSuffix());

        var serializedParameters = JsonMapperTestUtils.createMapper().valueToTree(details)
                .get("signatures").get(0).get("parameters");
        for (var index = 0; index < parameters.length; index++) {
            assertEquals(expectedLabels[index], parameters[index].label());
            assertEquals(index == 1, serializedParameters.get(index).get("optionsAsSentencePart").booleanValue());
        }
        assertEquals(0, parameters[0].options().length);
        assertEquals(0, parameters[2].options().length);
        assertEquals(6, parameters[1].options().length);
        for (var index = 0; index < OPERATIONS.length; index++) {
            var option = parameters[1].options()[index];
            assertEquals(OPERATIONS[index], option.value());
            assertEquals(OPTION_LABELS[index], option.label());
        }
    }

    @Test
    void pluginConstructorsShouldKeepLiteralOptionsUntilExplicitlyMarkedAsSentenceParts() {
        var option = new NoCodeParameterOption("Bezeichnung", "value");
        var original = new NoCodeParameter(NoCodeDataType.String, "Wert", "Ein Text.", option);
        var withSentence = new NoCodeParameter(NoCodeDataType.String, "Wert", "Ein Text.", "wenn", "ist", option);
        var withMetadata = new NoCodeParameter(
                NoCodeDataType.String, "Wert", "Ein Text.", "wenn", "ist", null, null,
                new NoCodeParameterOption[]{option}
        );

        assertFalse(original.optionsAsSentencePart());
        assertFalse(withSentence.optionsAsSentencePart());
        assertFalse(withMetadata.optionsAsSentencePart());
        assertTrue(withSentence.withOptionsAsSentencePart().withSentence("").optionsAsSentencePart());
        assertEquals(option, withMetadata.options()[0]);
    }

    @ParameterizedTest
    @MethodSource("comparisonCases")
    void allComparisonModesShouldKeepTheirOrderingAndEqualitySemantics(
            NoCodeTemporalCompareOperator.TemporalType type,
            String operation,
            boolean earlier,
            boolean equal,
            boolean later
    ) throws NoCodeException {
        var operator = new NoCodeTemporalCompareOperator(type);
        var values = sampleValues(type);

        assertEquals(earlier, operator.evaluate(runtime(), values[0], operation, values[1]).getValue());
        assertEquals(equal, operator.evaluate(runtime(), values[1], operation, values[1]).getValue());
        assertEquals(later, operator.evaluate(runtime(), values[2], operation, values[1]).getValue());
    }

    private static Stream<Arguments> comparisonCases() {
        return Stream.of(NoCodeTemporalCompareOperator.TemporalType.values()).flatMap(type -> Stream.of(
                Arguments.of(type, "gleich", false, true, false),
                Arguments.of(type, "ungleich", true, false, true),
                Arguments.of(type, "vor", true, false, false),
                Arguments.of(type, "vor-oder-gleich", true, true, false),
                Arguments.of(type, "nach", false, false, true),
                Arguments.of(type, "nach-oder-gleich", false, true, true)
        ));
    }

    @ParameterizedTest
    @EnumSource(NoCodeTemporalCompareOperator.TemporalType.class)
    void dynamicallyCalculatedComparisonModesShouldStillEvaluate(NoCodeTemporalCompareOperator.TemporalType type) {
        var operator = new NoCodeTemporalCompareOperator(type);
        var values = sampleValues(type);
        var expression = NoCodeExpression.of(
                operator.getIdentifier(),
                NoCodeStaticValue.of(values[0]),
                NoCodeExpression.of("value", NoCodeStaticValue.of("  VOR  ")),
                NoCodeStaticValue.of(values[1])
        );

        assertTrue(new NoCodeEvaluationService(List.of(provider())).evaluate(expression, runtime()).getValueAsBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"gleich", "ungleich", "vor", "vor-oder-gleich", "nach", "nach-oder-gleich"})
    void everyDateComparisonShouldExplainMismatchedCalendarComponents(String operation) {
        var operator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATE);

        assertEquals(
                "Verwenden Sie zwei vollständige Datumsangaben, zwei Angaben mit Monat und Jahr oder zwei Jahresangaben.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), "2026-11", operation, "2026-11-22")).getMessage()
        );
    }

    @ParameterizedTest
    @CsvSource({"2026-11, vor, 2026-12", "2026, gleich, 2026"})
    void datesShouldContinueToSupportMatchingMonthAndYearValues(String first, String operation, String second) throws NoCodeException {
        var operator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATE);

        assertTrue(operator.evaluate(runtime(), first, operation, second).getValueAsBoolean());
    }

    @Test
    void clockTimesShouldCompareWholeSecondsWithinOneDay() throws NoCodeException {
        var operator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.TIME);

        assertTrue(operator.evaluate(runtime(), "09:00", "gleich", "09:00:00").getValueAsBoolean());
        assertTrue(operator.evaluate(runtime(), LocalTime.of(9, 0, 0, 123), "gleich", LocalTime.of(9, 0, 0, 999)).getValueAsBoolean());
        assertFalse(operator.evaluate(runtime(), "23:00:00", "vor", "01:00:00").getValueAsBoolean());
    }

    @Test
    void instantsShouldCompareTheActualMomentAndRetainFractionalPrecision() throws NoCodeException {
        var operator = new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATETIME);

        assertTrue(operator.evaluate(runtime(), "2026-11-22T13:30:00+01:00", "gleich", "2026-11-22T12:30:00Z").getValueAsBoolean());
        assertTrue(operator.evaluate(runtime(), "2026-11-22T12:30:00.123456788Z", "vor", "2026-11-22T12:30:00.123456789Z").getValueAsBoolean());
        assertFalse(operator.evaluate(runtime(), "2026-11-22T12:30:00.123456788Z", "gleich", "2026-11-22T12:30:00.123456789Z").getValueAsBoolean());
    }

    @ParameterizedTest
    @EnumSource(NoCodeTemporalCompareOperator.TemporalType.class)
    void invalidValuesShouldIdentifyTheInputThatNeedsCorrection(NoCodeTemporalCompareOperator.TemporalType type) {
        var operator = new NoCodeTemporalCompareOperator(type);
        var valid = sampleValues(type)[1];
        var expectedMessages = switch (type) {
            case DATE -> new String[]{"Prüfen Sie die Datumsangabe für „Datum“.", "Prüfen Sie die Datumsangabe für „Vergleichsdatum“."};
            case TIME -> new String[]{"Prüfen Sie die Uhrzeit für „Uhrzeit“.", "Prüfen Sie die Uhrzeit für „Vergleichsuhrzeit“."};
            case DATETIME -> new String[]{"Prüfen Sie das Datum und die Uhrzeit für „Zeitpunkt“.", "Prüfen Sie das Datum und die Uhrzeit für „Vergleichszeitpunkt“."};
        };

        assertEquals(expectedMessages[0], assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), "invalid", "gleich", valid)).getMessage());
        assertEquals(expectedMessages[1], assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), valid, "gleich", "invalid")).getMessage());
        assertEquals(
                "Die Vergleichsart ist ungültig. Wählen Sie eine der angebotenen Vergleichsarten oder prüfen Sie die verwendete Wertquelle.",
                assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), valid, "invalid", valid)).getMessage()
        );
    }

    private static String[] sampleValues(NoCodeTemporalCompareOperator.TemporalType type) {
        return switch (type) {
            case DATE -> new String[]{"2026-11-21", "2026-11-22", "2026-11-23"};
            case TIME -> new String[]{"09:00:00", "17:00:00", "18:00:00"};
            case DATETIME -> new String[]{"2026-11-22T12:00:00+01:00", "2026-11-22T13:30:00+01:00", "2026-11-22T14:00:00+01:00"};
        };
    }

    private CommonOperatorsV1 provider() {
        return new CommonOperatorsV1(null, null, new BusinessTime(TEST_ZONE, Clock.fixed(Instant.parse("2026-11-22T12:30:00Z"), TEST_ZONE)));
    }
}
