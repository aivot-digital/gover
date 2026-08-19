package de.aivot.prosuna.backend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessEdgeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessEdgeControllerTest {
    @Test
    void updatePreservesExistingProcessScope() throws ResponseException {
        var jwt = mock(Jwt.class);
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");
        when(user.getFullName()).thenReturn("Test User");
        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));

        var existingEdge = new ProcessEdgeEntity(7, 12, 5, 1, 2, "existing-port");
        var processEdgeService = mock(ProcessEdgeService.class);
        when(processEdgeService.retrieve(7)).thenReturn(Optional.of(existingEdge));
        when(processEdgeService.update(eq(7), any(ProcessEdgeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var permissionService = mock(PermissionService.class);
        var objectMapper = mock(ObjectMapper.class);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(Map.of());
        var controller = new ProcessEdgeController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                processEdgeService,
                objectMapper,
                permissionService
        );
        var submittedEdge = new ProcessEdgeEntity(99, 42, 9, 3, 4, "updated-port");

        var result = controller.update(jwt, 7, submittedEdge);

        assertEquals(7, result.getId());
        assertEquals(12, result.getProcessId());
        assertEquals(5, result.getProcessVersion());
        verify(permissionService).requireProcessPermission(
                "user-1",
                12,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
        verify(processEdgeService).update(7, submittedEdge);
    }
}
