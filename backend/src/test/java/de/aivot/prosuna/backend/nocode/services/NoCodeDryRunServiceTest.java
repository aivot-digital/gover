package de.aivot.prosuna.backend.nocode.services;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.Collections;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunRequestDTO;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoCodeDryRunServiceTest {
    private final NoCodeEvaluationService evaluationService = mock(NoCodeEvaluationService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final NoCodeDryRunService service = new NoCodeDryRunService(evaluationService, permissionService, JsonMapperTestUtils.createMapper());

    private final NoCodeDryRunRequestDTO request = new NoCodeDryRunRequestDTO(
            new NoCodeStaticValue("a"),
            null,
            Map.of("anzahl", 2),
            Map.of("status", "neu"),
            Map.of("pruefung", Map.of("ok", true))
    );

    @Test
    void shouldRejectUsersWhoCannotAuthorExpressionsInAnyProcess() {
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE))
                .thenReturn(false);

        var exception = assertThrows(ResponseException.class, () -> service.dryRun("user", request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(evaluationService);
    }

    @Test
    void shouldRejectMissingUsers() {
        assertThrows(ResponseException.class, () -> service.dryRun(null, request));
        verifyNoInteractions(evaluationService);
    }

    @Test
    void shouldRunWithTheTestDataOfAnAuthorizedUser() throws ResponseException {
        // hasInAnyProcessPermission covers process grants and the system-level permission alike.
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE))
                .thenReturn(true);
        var expected = NoCodeDryRunResultDTO.evaluated("a", List.of());
        when(evaluationService.dryRun(any(), any(), any(), any())).thenReturn(expected);

        var result = service.dryRun("user", request);

        assertSame(expected, result);
        var context = ArgumentCaptor.forClass(ProcessExecutionData.class);
        verify(evaluationService).dryRun(any(NoCodeStaticValue.class), any(DerivedRuntimeElementData.class), context.capture(), any());
        assertEquals(Map.of("anzahl", 2), context.getValue().getProcessData());
        assertEquals(Map.of("status", "neu"), context.getValue().getProcessMetadata());
        assertEquals(Map.of("pruefung", Map.of("ok", true)), context.getValue().getNodeData());
    }

    @Test
    void rejectsOversizedInputWithoutEvaluationAndReleasesTheTestSlot() throws ResponseException {
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        var oversized = new NoCodeDryRunRequestDTO(new NoCodeStaticValue("x".repeat(NoCodeDryRunLimits.MAX_REQUEST_BYTES)), null, null, null, null);
        for (int i = 0; i < 6; i++) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseException.class, () -> service.dryRun("user", oversized)).getStatus());
        }
        verifyNoInteractions(evaluationService);
        var expected = NoCodeDryRunResultDTO.evaluated("a", List.of());
        when(evaluationService.dryRun(any(), any(), any(), any())).thenReturn(expected);
        assertSame(expected, service.dryRun("user", request));
    }

    @Test
    void rejectsOversizedExpressionsWithoutEvaluation() {
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        var expression = new NoCodeExpression();
        expression.setOperands(Collections.nCopies(513, new NoCodeStaticValue("a")));
        var oversized = new NoCodeDryRunRequestDTO(expression, null, null, null, null);
        assertThrows(ResponseException.class, () -> service.dryRun("user", oversized));
        verifyNoInteractions(evaluationService);
    }

    @Test
    void boundsConcurrentTestsAndReleasesSlotsAfterFailure() throws Exception {
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        var entered = new CountDownLatch(4);
        var release = new CountDownLatch(1);
        when(evaluationService.dryRun(any(), any(), any(), any())).thenAnswer(call -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Test timed out");
            throw new IllegalStateException("evaluation failed");
        });
        var executor = Executors.newFixedThreadPool(4);
        try {
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < 4; i++) futures.add(executor.submit(() -> assertThrows(IllegalStateException.class, () -> service.dryRun("user", request))));
            org.junit.jupiter.api.Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ResponseException.class, () -> service.dryRun("user", request)).getStatus());
            release.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            var expected = NoCodeDryRunResultDTO.evaluated("a", List.of());
            org.mockito.Mockito.doReturn(expected).when(evaluationService).dryRun(any(), any(), any(), any());
            assertSame(expected, service.dryRun("user", request));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1048576, 262144})
    void stopsOversizedIntermediateResultsBeforeEvaluatingTheirParent(int valueBytes) throws Exception {
        when(permissionService.hasInAnyProcessPermission("user", ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)).thenReturn(true);
        var child = mock(NoCodeOperator.class);
        var parent = mock(NoCodeOperator.class);
        when(child.getIdentifier()).thenReturn("large");
        when(parent.getIdentifier()).thenReturn("parent");
        when(child.evaluate(any(), any(Object[].class))).thenReturn(new NoCodeResult("x".repeat(valueBytes)));
        var provider = mock(NoCodeOperatorsProvider.class);
        when(provider.getOperators()).thenReturn(new NoCodeOperator[]{child, parent});
        when(provider.supportsDryRun(any())).thenReturn(true);
        var bounded = new NoCodeDryRunService(new NoCodeEvaluationService(List.of(provider)), permissionService, JsonMapperTestUtils.createMapper());
        var inner = new NoCodeExpression();
        inner.setOperatorIdentifier("large");
        inner.setOperands(List.of());
        var outer = new NoCodeExpression();
        outer.setOperatorIdentifier("parent");
        outer.setOperands(Collections.nCopies(valueBytes == NoCodeDryRunLimits.MAX_RESULT_BYTES ? 1 : 8, inner));
        var result = bounded.dryRun("user", new NoCodeDryRunRequestDTO(outer, null, null, null, null));
        assertEquals(NoCodeDryRunStatus.Skipped, result.status());
        org.junit.jupiter.api.Assertions.assertTrue(result.operands().stream().anyMatch(item -> item.message() != null && item.message().contains("1 MiB")));
        org.junit.jupiter.api.Assertions.assertTrue(JsonMapperTestUtils.createMapper().writeValueAsBytes(result).length <= NoCodeDryRunLimits.MAX_RESULT_BYTES);
        org.mockito.Mockito.verify(parent, org.mockito.Mockito.never()).evaluate(any(), any(Object[].class));
    }

}
