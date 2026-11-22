package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunRequestDTO;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeAddOperator;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class NoCodeEvaluationServiceDryRunTest {
    private final NoCodeEvaluationService service = new NoCodeEvaluationService(List.of(
            new CommonOperatorsV1(null, null, new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC()))
    ));

    private NoCodeDryRunResultDTO dryRun(NoCodeExpression expression) {
        return service.dryRun(expression, new DerivedRuntimeElementData(), new ProcessExecutionData());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void ifGuardsDivisionByZeroInBothEvaluationPaths(int count) {
        var countReference = new NoCodeProcessDataReference("anzahl");
        var expression = new NoCodeExpression("if",
                new NoCodeExpression("greater-than", countReference, new NoCodeStaticValue(0)),
                new NoCodeExpression("divide", new NoCodeProcessDataReference("gesamtbetrag"), countReference),
                new NoCodeStaticValue("Keine Einträge"));
        var processData = new ProcessExecutionData().addProcessData("anzahl", count).addProcessData("gesamtbetrag", 24);
        var elementData = new DerivedRuntimeElementData();
        var expected = count == 0 ? "Keine Einträge" : new BigDecimal("8.00000000");

        assertEquals(expected, service.evaluate(expression, elementData, processData).getValue());
        var result = service.dryRun(expression, elementData, processData);
        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals(expected, result.value());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifReportsAllThreeInputsWithoutReadingTheUnselectedProtectedBranch(boolean condition) {
        var key = spy(new NoCodeReference("key"));
        var secret = new NoCodeExpression("secrets-get", key);
        var selected = new NoCodeExpression("value", new NoCodeStaticValue("selected"));
        var expression = new NoCodeExpression("if", new NoCodeStaticValue(condition),
                condition ? selected : secret, condition ? secret : selected);

        var result = dryRun(expression);

        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals("selected", result.value());
        assertEquals(3, result.operands().size());
        assertEquals(NoCodeDryRunResultDTO.evaluated(condition, List.of()), result.operands().getFirst());
        assertEquals(dryRun(selected), result.operands().get(condition ? 1 : 2));
        var reason = condition ? "Die Bedingung ist wahr." : "Die Bedingung ist falsch.";
        assertEquals(NoCodeDryRunResultDTO.skipped(reason, List.of()), result.operands().get(condition ? 2 : 1));
        verify(key, never()).getElementId();
    }

    private static Stream<Arguments> unavailableOperands() {
        return Stream.of(
                Arguments.of(new NoCodeExpression("divide", new NoCodeStaticValue(1), new NoCodeStaticValue(0)),
                        NoCodeDryRunStatus.Failed, "„Teiler“ darf nicht 0 sein. Geben Sie eine andere Zahl ein."),
                Arguments.of(new NoCodeExpression("secrets-get", new NoCodeStaticValue("key")),
                        NoCodeDryRunStatus.Skipped, "Diese Funktion wird im Testlauf nicht ausgeführt, weil sie geschützte Daten liest oder Aktionen auslösen kann.")
        );
    }

    @ParameterizedTest
    @MethodSource("unavailableOperands")
    void ifSkipsBothBranchesWhenTheConditionIsUnavailable(NoCodeExpression condition, NoCodeDryRunStatus status, String message) {
        var unread = spy(new NoCodeReference("unused"));
        var branch = new NoCodeExpression("value", unread);

        var result = dryRun(new NoCodeExpression("if", condition, branch, branch));

        var reason = "Die Bedingung konnte nicht ausgewertet werden.";
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals(reason, result.message());
        assertEquals(3, result.operands().size());
        assertEquals(status, result.operands().getFirst().status());
        assertEquals(message, result.operands().getFirst().message());
        var skipped = NoCodeDryRunResultDTO.skipped(reason, List.of());
        assertEquals(List.of(skipped, skipped), result.operands().subList(1, 3));
        verify(unread, never()).getElementId();
    }

    @ParameterizedTest
    @MethodSource("unavailableOperands")
    void ifKeepsTheReasonTheSelectedBranchWasUnavailable(NoCodeExpression selected, NoCodeDryRunStatus status, String message) {
        var result = dryRun(new NoCodeExpression("if", new NoCodeStaticValue(false),
                new NoCodeExpression("missing"), selected));

        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals("Das gewählte Ergebnis konnte nicht ausgewertet werden.", result.message());
        assertEquals(3, result.operands().size());
        assertEquals(NoCodeDryRunResultDTO.evaluated(false, List.of()), result.operands().getFirst());
        assertEquals(NoCodeDryRunResultDTO.skipped("Die Bedingung ist falsch.", List.of()), result.operands().get(1));
        assertEquals(status, result.operands().get(2).status());
        assertEquals(message, result.operands().get(2).message());
    }

    @Test
    void ifHonorsTheResultBudgetBeforeUsingTheSelectedValue() {
        var limited = NoCodeDryRunResultDTO.skipped("limit reached", List.of());
        var expression = new NoCodeExpression("if", new NoCodeStaticValue(true),
                new NoCodeStaticValue("selected"), new NoCodeStaticValue("unused"));

        var result = service.dryRun(expression, new DerivedRuntimeElementData(), new ProcessExecutionData(),
                intermediate -> "selected".equals(intermediate.value()) ? limited : intermediate);

        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals("Das gewählte Ergebnis konnte nicht ausgewertet werden.", result.message());
        assertEquals(limited, result.operands().get(1));
        assertEquals(NoCodeDryRunResultDTO.skipped("Die Bedingung ist wahr.", List.of()), result.operands().get(2));
    }

    @ParameterizedTest
    @CsvSource({"0, false", "3, true", "4, false"})
    void andGuardsDivisionByZeroInBothEvaluationPaths(int count, boolean expected) {
        var countReference = new NoCodeProcessDataReference("anzahl");
        var average = new NoCodeExpression("divide", new NoCodeProcessDataReference("gesamtbetrag"), countReference);
        var expression = new NoCodeExpression("and",
                new NoCodeExpression("greater-than", countReference, new NoCodeStaticValue(0)),
                new NoCodeExpression("greater-than", average, new NoCodeStaticValue(10)));
        var processData = new ProcessExecutionData().addProcessData("anzahl", count).addProcessData("gesamtbetrag", 36);
        var elementData = new DerivedRuntimeElementData();

        assertEquals(expected, service.evaluate(expression, elementData, processData).getValue());
        var result = service.dryRun(expression, elementData, processData);
        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals(expected, result.value());
        assertEquals(2, result.operands().size());
        assertEquals(NoCodeDryRunStatus.Evaluated, result.operands().getFirst().status());
        assertEquals(count > 0, result.operands().getFirst().value());
        var second = result.operands().get(1);
        if (count == 0) {
            assertEquals(NoCodeDryRunResultDTO.skipped("Die erste Bedingung ist falsch.", List.of()), second);
        } else {
            assertEquals(NoCodeDryRunStatus.Evaluated, second.status());
            assertEquals(expected, second.value());
            assertEquals(List.of(36, count), second.operands().getFirst().operands().stream().map(NoCodeDryRunResultDTO::value).toList());
        }
    }

    @ParameterizedTest
    @MethodSource("unavailableOperands")
    void andSkipsTheSecondConditionWhenTheFirstIsUnavailable(NoCodeExpression first, NoCodeDryRunStatus status, String message) {
        var unread = spy(new NoCodeReference("unused"));
        var result = dryRun(new NoCodeExpression("and", first, new NoCodeExpression("value", unread)));

        var reason = "Die erste Bedingung konnte nicht ausgewertet werden.";
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals(reason, result.message());
        assertEquals(2, result.operands().size());
        assertEquals(status, result.operands().getFirst().status());
        assertEquals(message, result.operands().getFirst().message());
        assertEquals(NoCodeDryRunResultDTO.skipped(reason, List.of()), result.operands().get(1));
        verify(unread, never()).getElementId();
    }

    @ParameterizedTest
    @MethodSource("unavailableOperands")
    void andKeepsTheReasonTheSecondConditionWasUnavailable(NoCodeExpression second, NoCodeDryRunStatus status, String message) {
        var result = dryRun(new NoCodeExpression("and", new NoCodeStaticValue(true), second));

        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals("Die zweite Bedingung konnte nicht ausgewertet werden.", result.message());
        assertEquals(2, result.operands().size());
        assertEquals(NoCodeDryRunResultDTO.evaluated(true, List.of()), result.operands().getFirst());
        assertEquals(status, result.operands().get(1).status());
        assertEquals(message, result.operands().get(1).message());
    }

    @Test
    void andAppliesTheResultBudgetBeforeTreatingTheFirstConditionAsFalse() {
        var limited = NoCodeDryRunResultDTO.skipped("limit reached", List.of());
        var unread = spy(new NoCodeReference("unused"));
        var expression = new NoCodeExpression("and", new NoCodeStaticValue(false), new NoCodeExpression("value", unread));

        var result = service.dryRun(expression, new DerivedRuntimeElementData(), new ProcessExecutionData(),
                intermediate -> Boolean.FALSE.equals(intermediate.value()) ? limited : intermediate);

        var reason = "Die erste Bedingung konnte nicht ausgewertet werden.";
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals(reason, result.message());
        assertEquals(List.of(limited, NoCodeDryRunResultDTO.skipped(reason, List.of())), result.operands());
        verify(unread, never()).getElementId();
    }

    @ParameterizedTest
    @CsvSource({"0, true", "3, true", "4, false"})
    void orGuardsDivisionByZeroInBothEvaluationPaths(int count, boolean expected) {
        var countReference = new NoCodeProcessDataReference("anzahl");
        var average = new NoCodeExpression("divide", new NoCodeProcessDataReference("gesamtbetrag"), countReference);
        var expression = new NoCodeExpression("or",
                new NoCodeExpression("less-than-or-equal", countReference, new NoCodeStaticValue(0)),
                new NoCodeExpression("greater-than", average, new NoCodeStaticValue(10)));
        var processData = new ProcessExecutionData().addProcessData("anzahl", count).addProcessData("gesamtbetrag", 36);
        var elementData = new DerivedRuntimeElementData();

        assertEquals(expected, service.evaluate(expression, elementData, processData).getValue());
        var result = service.dryRun(expression, elementData, processData);
        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals(expected, result.value());
        assertEquals(2, result.operands().size());
        assertEquals(count <= 0, result.operands().getFirst().value());
        var second = result.operands().get(1);
        if (count == 0) {
            assertEquals(NoCodeDryRunResultDTO.skipped("Bedingung 1 ist wahr.", List.of()), second);
        } else {
            assertEquals(NoCodeDryRunStatus.Evaluated, second.status());
            assertEquals(expected, second.value());
        }
    }

    @Test
    void orKeepsAllTenResultPositionsWithoutVisitingConditionsAfterTheThirdIsTrue() {
        var unread = spy(new NoCodeReference("unused"));
        var conditions = new ArrayList<NoCodeOperand>(List.of(
                new NoCodeStaticValue(false), new NoCodeStaticValue(0), new NoCodeStaticValue("wahr")));
        conditions.addAll(Collections.nCopies(7, new NoCodeExpression("secrets-get", unread)));

        var result = dryRun(new NoCodeExpression("or").setOperands(conditions));

        var expected = new ArrayList<>(List.of(NoCodeDryRunResultDTO.evaluated(false, List.of()),
                NoCodeDryRunResultDTO.evaluated(0, List.of()), NoCodeDryRunResultDTO.evaluated("wahr", List.of())));
        expected.addAll(Collections.nCopies(7, NoCodeDryRunResultDTO.skipped("Bedingung 3 ist wahr.", List.of())));
        assertEquals(NoCodeDryRunResultDTO.evaluated(true, expected), result);
        verify(unread, never()).getElementId();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
    void orReturnsFalseAfterEvaluatingEveryConditionForEachSupportedArity(int count) {
        var expression = new NoCodeExpression("or").setOperands(Collections.nCopies(count, new NoCodeStaticValue(false)));

        assertEquals(false, service.evaluate(expression, new DerivedRuntimeElementData()).getValue());
        var evaluatedConditions = Collections.nCopies(count, NoCodeDryRunResultDTO.evaluated(false, List.of()));
        assertEquals(NoCodeDryRunResultDTO.evaluated(false, evaluatedConditions), dryRun(expression));
    }

    @ParameterizedTest
    @MethodSource("unavailableOperands")
    void orReportsTheUnavailableConditionAndSkipsEveryFollowingInput(NoCodeExpression unavailable, NoCodeDryRunStatus status, String message) {
        var unread = spy(new NoCodeReference("unused"));
        var remaining = new NoCodeExpression("value", unread);
        var expression = new NoCodeExpression("or", new NoCodeStaticValue(false), unavailable, remaining, remaining);

        var result = dryRun(expression);

        var reason = "Bedingung 2 konnte nicht ausgewertet werden.";
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals(reason, result.message());
        assertEquals(4, result.operands().size());
        assertEquals(NoCodeDryRunResultDTO.evaluated(false, List.of()), result.operands().getFirst());
        assertEquals(status, result.operands().get(1).status());
        assertEquals(message, result.operands().get(1).message());
        assertEquals(Collections.nCopies(2, NoCodeDryRunResultDTO.skipped(reason, List.of())), result.operands().subList(2, 4));
        verify(unread, never()).getElementId();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 11})
    void orRejectsUnsupportedConditionCountsEvenWhenTheFirstValueWouldBeTrue(int count) {
        var expression = new NoCodeExpression("or").setOperands(Collections.nCopies(count, new NoCodeStaticValue(true)));

        var error = assertThrows(IllegalStateException.class, () -> service.evaluate(expression, new DerivedRuntimeElementData()));
        assertInstanceOf(NoCodeWrongArgumentCountException.class, error.getCause());
        var result = dryRun(expression);
        assertEquals(NoCodeDryRunStatus.Failed, result.status());
        assertEquals("Die Anzahl der Parameter passt nicht zur Funktion.", result.message());
    }

    @Test
    void shouldRetainASingleSpaceSeparatorAcrossSerializationAndBothEvaluationPaths() {
        var mapper = JsonMapperTestUtils.createMapper();
        var request = mapper.readValue("""
                {
                  "expression": {
                    "type": "NoCodeExpression",
                    "operatorIdentifier": "split",
                    "operands": [
                      {"type": "NoCodeStaticValue", "value": "Max Mustermann"},
                      {"type": "NoCodeStaticValue", "value": " "}
                    ]
                  }
                }
                """, NoCodeDryRunRequestDTO.class);

        var serialized = mapper.writeValueAsString(request);
        assertEquals(" ", mapper.readTree(serialized).at("/expression/operands/1/value").asString());
        var restored = mapper.readValue(serialized, NoCodeDryRunRequestDTO.class);
        var data = new DerivedRuntimeElementData();
        var expected = List.of("Max", "Mustermann");
        assertEquals(expected, service.evaluate(restored.expression(), data).getValue());
        var result = service.dryRun(restored.expression(), data, new ProcessExecutionData());
        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals(expected, result.value());
        assertEquals(" ", result.operands().get(1).value());
    }

    @Test
    void shouldRetainWhitespaceInAdditionalConcatOperandsWithoutChangingUnmarkedInputs() {
        var expression = new NoCodeExpression("concat",
                new NoCodeExpression("value", new NoCodeStaticValue(" ")),
                new NoCodeStaticValue("Max"), new NoCodeStaticValue(" "), new NoCodeStaticValue("Mustermann"));

        assertEquals("Max Mustermann", service.evaluate(expression, new DerivedRuntimeElementData()).getValue());
        var result = dryRun(expression);
        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals("Max Mustermann", result.value());
        assertEquals(" ", result.operands().get(2).value());
        var unmarked = result.operands().getFirst();
        assertNull(unmarked.value());
        assertNull(unmarked.operands().getFirst().value());
    }

    @Test
    void shouldKeepTheWholeTextWhenTheSeparatorIsEmpty() {
        var expression = new NoCodeExpression("split", new NoCodeStaticValue("Max Mustermann"), new NoCodeStaticValue(""));
        assertEquals(List.of("Max Mustermann"), service.evaluate(expression, new DerivedRuntimeElementData()).getValue());
        var result = dryRun(expression);
        assertEquals(List.of("Max Mustermann"), result.value());
        assertNull(result.operands().get(1).value());
    }

    @Test
    void shouldUseExplicitVisibilityTestStatesForBothOperators() {
        for (var visible : List.of(true, false)) {
            var data = new DerivedRuntimeElementData();
            data.getElementStates().put("field", ComputedElementState.create().setVisible(visible));
            for (var identifier : List.of("is-visible", "is-invisible")) {
                var result = service.dryRun(new NoCodeExpression(identifier, new NoCodeStaticValue("field")), data, new ProcessExecutionData());
                assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
                assertEquals(identifier.equals("is-visible") ? visible : !visible, result.value());
            }
        }
    }

    @Test
    void shouldKeepTheResultOfEveryOperand() {
        var processData = new ProcessExecutionData().addProcessData("anzahl", 2);

        var result = service.dryRun(
                new NoCodeExpression("add", new NoCodeStaticValue(1), new NoCodeExpression(
                        "add",
                        new NoCodeProcessDataReference("anzahl"),
                        new NoCodeStaticValue(3)
                )),
                new DerivedRuntimeElementData(),
                processData
        );

        assertEquals(NoCodeDryRunStatus.Evaluated, result.status());
        assertEquals(0, new BigDecimal(6).compareTo((BigDecimal) result.value()));
        var inner = result.operands().get(1);
        assertEquals(0, new BigDecimal(5).compareTo((BigDecimal) inner.value()));
        assertEquals(List.of(2, 3), inner.operands().stream().map(NoCodeDryRunResultDTO::value).toList());
    }

    @Test
    void shouldSerializeTheTypesOfTemporalSubexpressionsBeforeTheirValuesBecomeStrings() {
        var result = dryRun(new NoCodeExpression("compare-datetime",
                new NoCodeExpression("add-to-datetime", new NoCodeExpression("create-now"),
                        new NoCodeStaticValue(1), new NoCodeStaticValue("tage")),
                new NoCodeStaticValue("vor"), new NoCodeStaticValue("2026-11-22T13:30:00Z")));

        var mapper = JsonMapperTestUtils.createMapper();
        var json = mapper.readTree(mapper.writeValueAsString(result));
        assertEquals("Evaluated", json.get("status").asString());
        assertEquals(result.value(), json.get("value").asBoolean());
        var adjusted = json.at("/operands/0");
        assertEquals("DateTime", adjusted.get("valueType").asString());
        assertEquals("DateTime", adjusted.at("/operands/0/valueType").asString());
        var literal = json.at("/operands/2");
        assertEquals("2026-11-22T13:30:00Z", literal.get("value").asString());
        assertNull(literal.get("valueType"));
    }

    @Test
    void shouldIdentifyCalendarAndClockResultsAtTheirOriginalPrecision() {
        assertEquals(NoCodeDataType.Date, dryRun(new NoCodeExpression("create-today")).getValueType());
        for (var date : List.of("2026-11", "2026")) {
            var result = dryRun(new NoCodeExpression("subtract-from-date", new NoCodeStaticValue(date),
                    new NoCodeStaticValue(0), new NoCodeStaticValue("jahre")));
            assertEquals(NoCodeDataType.Date, result.getValueType());
            assertEquals(date, result.value().toString());
        }
        var time = dryRun(new NoCodeExpression("extract-time-from-datetime", new NoCodeExpression("create-now")));
        assertEquals(NoCodeDataType.Time, time.getValueType());
    }

    @Test
    void shouldSkipOperatorsReadingProtectedDataAndEverythingDependingOnThem() {
        var result = dryRun(new NoCodeExpression(
                "equals",
                new NoCodeExpression("secrets-get", new NoCodeStaticValue("api-key")),
                new NoCodeStaticValue("geheim")
        ));

        var secret = result.operands().getFirst();
        assertEquals(NoCodeDryRunStatus.Skipped, secret.status());
        assertEquals(
                "Diese Funktion wird im Testlauf nicht ausgeführt, weil sie geschützte Daten liest oder Aktionen auslösen kann.",
                secret.message()
        );
        assertNull(secret.value());
        assertEquals(NoCodeDryRunStatus.Evaluated, secret.operands().getFirst().status());
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        assertEquals(NoCodeDryRunStatus.Evaluated, result.operands().get(1).status());
    }

    @Test
    void shouldReportOperatorFailuresPerOperand() {
        var result = dryRun(new NoCodeExpression(
                "add",
                new NoCodeExpression("divide", new NoCodeStaticValue(1), new NoCodeStaticValue(0)),
                new NoCodeStaticValue(1)
        ));

        var division = result.operands().getFirst();
        assertEquals(NoCodeDryRunStatus.Failed, division.status());
        assertEquals("„Teiler“ darf nicht 0 sein. Geben Sie eine andere Zahl ein.", division.message());
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
    }

    @Test
    void shouldReportUnknownOperators() {
        var result = dryRun(new NoCodeExpression("missing"));

        assertEquals(NoCodeDryRunStatus.Failed, result.status());
        assertEquals("Die Funktion ist nicht verfügbar.", result.message());
    }

    @Test
    void shouldExplainInvalidFunctionParameterCounts() {
        var result = dryRun(new NoCodeExpression("add", new NoCodeStaticValue(1)));

        assertEquals(NoCodeDryRunStatus.Failed, result.status());
        assertEquals("Die Anzahl der Parameter passt nicht zur Funktion.", result.message());
    }

    @Test
    void shouldSkipOperatorsOfProvidersThatDoNotOptIn() {
        var provider = mock(NoCodeOperatorsProvider.class, CALLS_REAL_METHODS);
        doReturn(new NoCodeOperator[]{new NoCodeAddOperator()}).when(provider).getOperators();
        var pluginService = new NoCodeEvaluationService(List.of(provider));

        var result = pluginService.dryRun(
                new NoCodeExpression("add", new NoCodeStaticValue(1), new NoCodeStaticValue(2)),
                new DerivedRuntimeElementData(),
                ProcessExecutionData.of(Map.of())
        );

        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
    }

    @Test
    void shouldExcludeCoreOperatorsReadingSecretsOrUsers() {
        var provider = new CommonOperatorsV1(null, null, new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC()));

        var excluded = java.util.Arrays.stream(provider.getOperators())
                .filter(operator -> !provider.supportsDryRun(operator))
                .map(NoCodeOperator::getIdentifier)
                .sorted()
                .toList();

        assertEquals(List.of("secrets-get", "user-email", "user-full-name"), excluded);
    }
}
