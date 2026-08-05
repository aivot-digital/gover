package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.dtos.ProcessInstanceReassignRequestDTO;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import de.aivot.gover.backend.process.services.ProcessInstanceTaskService;
import de.aivot.gover.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceControllerTest {
    @Test
    void reassignUsesDedicatedPermissionAndUpdatesOnlyTheAssignee() throws ResponseException {
        var jwt = mock(Jwt.class);
        var executingUser = mock(UserEntity.class);
        when(executingUser.getId()).thenReturn("executing-user");
        when(executingUser.getFullName()).thenReturn("Executing User");

        var userService = mock(UserService.class);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(executingUser));
        when(userService.retrieve("00000000-0000-0000-0000-000000000042"))
                .thenReturn(Optional.of(mock(UserEntity.class)));

        var existing = new ProcessInstanceEntity()
                .setId(7L)
                .setProcessId(12)
                .setAssignedUserId(null);
        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.retrieve(7L)).thenReturn(Optional.of(existing));
        when(processInstanceService.save(existing)).thenReturn(existing);
        var permissionService = mock(PermissionService.class);
        var controller = new ProcessInstanceController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                processInstanceService,
                mock(ProcessInstanceTaskService.class),
                mock(RabbitTemplate.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
                permissionService
        );

        var result = controller.reassign(
                jwt,
                7L,
                new ProcessInstanceReassignRequestDTO("00000000-0000-0000-0000-000000000042")
        );

        assertEquals("00000000-0000-0000-0000-000000000042", result.getAssignedUserId());
        assertNotNull(result.getUpdated());
        verify(permissionService).requireProcessInstancePermission(
                "executing-user",
                7L,
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN
        );
        verify(processInstanceService).save(existing);
    }
}
