package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceReassignRequestDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceRestartService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessInstanceControllerTest {
    @Test
    void restartAuthorizesTheInstanceWithinTheRestartBeforeAuditing() throws Exception {
        var fixture = new RestartFixture();
        var restarted = new ProcessInstanceEntity().setId(7L).setProcessId(1).setStatus(ProcessInstanceStatus.Running);
        when(fixture.restarts.restartFailedInstance(eq(7L), eq("actor"), any())).thenAnswer(invocation -> {
            invocation.<ProcessTaskExecutionService.Authorization>getArgument(2).run();
            return restarted;
        });

        var result = fixture.controller.restartFailed(fixture.jwt, 7L);

        assertSame(restarted, result);
        // The restart service runs the authorization under the instance lock.
        var order = inOrder(fixture.restarts, fixture.permissions, fixture.scopedAudit);
        order.verify(fixture.restarts).restartFailedInstance(eq(7L), eq("actor"), any());
        order.verify(fixture.permissions).requireProcessInstancePermission("actor", 7L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_RESTART_FAILED);
        order.verify(fixture.scopedAudit).create();
    }

    @Test
    void deniedRestartIsRejectedAndNotAudited() throws Exception {
        var fixture = new RestartFixture();
        doThrow(ResponseException.forbidden()).when(fixture.permissions)
                .requireProcessInstancePermission("actor", 7L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_RESTART_FAILED);
        when(fixture.restarts.restartFailedInstance(eq(7L), eq("actor"), any())).thenAnswer(invocation -> {
            invocation.<ProcessTaskExecutionService.Authorization>getArgument(2).run();
            return new ProcessInstanceEntity().setId(7L).setProcessId(1).setStatus(ProcessInstanceStatus.Running);
        });

        var exception = assertThrows(ResponseException.class, () -> fixture.controller.restartFailed(fixture.jwt, 7L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(fixture.scopedAudit);
    }

    @Test
    void rejectedRestartIsNotAudited() throws Exception {
        var fixture = new RestartFixture();
        when(fixture.restarts.restartFailedInstance(eq(7L), eq("actor"), any())).thenThrow(ResponseException.conflict("Nicht möglich."));

        assertThrows(ResponseException.class, () -> fixture.controller.restartFailed(fixture.jwt, 7L));

        verifyNoInteractions(fixture.scopedAudit);
    }

    private static class RestartFixture {
        final Jwt jwt = mock(Jwt.class);
        final ProcessInstanceRestartService restarts = mock(ProcessInstanceRestartService.class);
        final PermissionService permissions = mock(PermissionService.class);
        final AuditService audit = mock(AuditService.class, RETURNS_DEEP_STUBS);
        final ScopedAuditService scopedAudit = mock(ScopedAuditService.class, RETURNS_DEEP_STUBS);
        final ProcessInstanceController controller;

        RestartFixture() throws Exception {
            var user = new UserEntity().setId("actor").setFullName("Alex Beispiel");
            var users = mock(UserService.class);
            when(users.fromJWT(jwt)).thenReturn(Optional.of(user));
            var instances = mock(ProcessInstanceService.class);
            var instance = new ProcessInstanceEntity().setId(7L).setProcessId(1).setStatus(ProcessInstanceStatus.Failed);
            when(instances.retrieve(7L)).thenReturn(Optional.of(instance));
            when(audit.createScopedAuditService(ProcessInstanceController.class, "Prozesse")).thenReturn(scopedAudit);
            controller = new ProcessInstanceController(audit, users, instances, restarts, permissions,
                    mock(de.aivot.prosuna.backend.process.services.ProcessAssignmentService.class));
        }
    }

    @Test
    void reassignDelegatesToPermissionProtectedAssignmentService() throws ResponseException {
        var jwt = mock(Jwt.class);
        var executingUser = mock(UserEntity.class);
        when(executingUser.getId()).thenReturn("executing-user");
        when(executingUser.getFullName()).thenReturn("Executing User");

        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(executingUser));
        var assignmentService = mock(de.aivot.prosuna.backend.process.services.ProcessAssignmentService.class);
        var expected = new ProcessInstanceEntity().setId(7L).setAssignedUserId("00000000-0000-0000-0000-000000000042");
        when(assignmentService.reassignInstance(executingUser, 7L, expected.getAssignedUserId())).thenReturn(expected);
        var controller = new ProcessInstanceController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                mock(ProcessInstanceService.class),
                mock(ProcessInstanceRestartService.class),
                mock(PermissionService.class), assignmentService
        );

        var result = controller.reassign(
                jwt,
                7L,
                new ProcessInstanceReassignRequestDTO("00000000-0000-0000-0000-000000000042")
        );

        assertEquals("00000000-0000-0000-0000-000000000042", result.getAssignedUserId());
        verify(assignmentService).reassignInstance(executingUser, 7L, expected.getAssignedUserId());
    }
}
