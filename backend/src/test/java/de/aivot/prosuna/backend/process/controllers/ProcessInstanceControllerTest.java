package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceReassignRequestDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceControllerTest {
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
                mock(ProcessInstanceTaskService.class),
                mock(RabbitTemplate.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
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
