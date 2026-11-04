package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
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
    void shouldSkipOperatorsReadingProtectedDataAndEverythingDependingOnThem() {
        var result = dryRun(new NoCodeExpression(
                "equals",
                new NoCodeExpression("secrets-get", new NoCodeStaticValue("api-key")),
                new NoCodeStaticValue("geheim")
        ));

        var secret = result.operands().getFirst();
        assertEquals(NoCodeDryRunStatus.Skipped, secret.status());
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
        assertEquals("Division durch Null ist nicht erlaubt", division.message());
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
    }

    @Test
    void shouldReportUnknownOperators() {
        var result = dryRun(new NoCodeExpression("missing"));

        assertEquals(NoCodeDryRunStatus.Failed, result.status());
        assertEquals("Der Operator ist nicht verfügbar.", result.message());
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
