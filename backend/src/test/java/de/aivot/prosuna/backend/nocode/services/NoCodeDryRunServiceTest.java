package de.aivot.prosuna.backend.nocode.services;

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
    private final NoCodeDryRunService service = new NoCodeDryRunService(evaluationService, permissionService);

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
        when(evaluationService.dryRun(any(), any(), any())).thenReturn(expected);

        var result = service.dryRun("user", request);

        assertSame(expected, result);
        var context = ArgumentCaptor.forClass(ProcessExecutionData.class);
        verify(evaluationService).dryRun(any(NoCodeStaticValue.class), any(DerivedRuntimeElementData.class), context.capture());
        assertEquals(Map.of("anzahl", 2), context.getValue().getProcessData());
        assertEquals(Map.of("status", "neu"), context.getValue().getProcessMetadata());
        assertEquals(Map.of("pruefung", Map.of("ok", true)), context.getValue().getNodeData());
    }
}
