package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunRequestDTO;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeAddOperator;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class NoCodeEvaluationServiceDryRunTest {
    private final NoCodeEvaluationService service = new NoCodeEvaluationService(List.of(
            new CommonOperatorsV1(null, null, new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC()))
    ));

    private NoCodeDryRunResultDTO dryRun(NoCodeExpression expression) {
        return service.dryRun(expression, new DerivedRuntimeElementData(), new ProcessExecutionData());
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
