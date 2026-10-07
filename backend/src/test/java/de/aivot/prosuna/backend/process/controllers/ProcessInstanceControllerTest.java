package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceReassignRequestDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessInstanceControllerTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restartQueuesTheLatestFailedTaskReferenceOrAnUnlinkedInitialStart(boolean hasTask) throws Exception {
        var fixture = new RestartFixture();
        when(fixture.tasks.retrieveLatestForInstanceId(7L))
                .thenReturn(hasTask ? Optional.of(fixture.task) : Optional.empty());

        var result = fixture.controller.restartFailed(fixture.jwt, 7L);

        var payload = ArgumentCaptor.forClass(ProcessWorker.DoWorkWorkerPayload.class);
        verify(fixture.rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE), payload.capture());
        assertEquals(7L, payload.getValue().processInstanceId());
        assertEquals(hasTask ? 34L : null, payload.getValue().restartForTaskId());
        assertEquals(hasTask ? 33L : null, payload.getValue().previousTaskId());
        assertEquals(hasTask ? 56 : 11, payload.getValue().nextNodeId());
        assertEquals(ProcessInstanceStatus.Running, result.getStatus());
        verify(fixture.permissions).requireProcessInstancePermission("actor", 7L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE);
        if (hasTask) {
            assertEquals(ProcessTaskStatus.Restarted, fixture.task.getStatus());
            assertEquals(32L, fixture.task.getRestartForTaskId());
            verify(fixture.tasks).save(fixture.task);
        }
    }

    @Test
    void failedQueueSubmissionDoesNotMarkTheTaskRestartedOrLogSuccess() throws Exception {
        var fixture = new RestartFixture();
        when(fixture.tasks.retrieveLatestForInstanceId(7L)).thenReturn(Optional.of(fixture.task));
        doThrow(new RuntimeException("Queue unavailable")).when(fixture.rabbit)
                .convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE), any(ProcessWorker.DoWorkWorkerPayload.class));

        assertThrows(ResponseException.class, () -> fixture.controller.restartFailed(fixture.jwt, 7L));
        assertEquals(ProcessTaskStatus.Failed, fixture.task.getStatus());
        verify(fixture.tasks, never()).save(any());
        verifyNoInteractions(fixture.loggers, fixture.audit.createScopedAuditService(ProcessInstanceController.class, "Prozesse"));
    }

    private static class RestartFixture {
        final Jwt jwt = mock(Jwt.class);
        final ProcessInstanceTaskService tasks = mock(ProcessInstanceTaskService.class);
        final RabbitTemplate rabbit = mock(RabbitTemplate.class);
        final PermissionService permissions = mock(PermissionService.class);
        final ProcessNodeExecutionLoggerFactory loggers = mock(ProcessNodeExecutionLoggerFactory.class, RETURNS_DEEP_STUBS);
        final AuditService audit = mock(AuditService.class, RETURNS_DEEP_STUBS);
        final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(34L).setProcessInstanceId(7L)
                .setProcessNodeId(56).setStatus(ProcessTaskStatus.Failed).setPreviousProcessInstanceTaskId(33L)
                .setPreviousProcessNodeId(55).setPreviousProcessNodePortKey("next").setRestartForTaskId(32L);
        final ProcessInstanceController controller;

        RestartFixture() throws Exception {
            var user = new UserEntity().setId("actor").setFullName("Alex Beispiel");
            var users = mock(UserService.class);
            when(users.fromJWT(jwt)).thenReturn(Optional.of(user));
            var instances = mock(ProcessInstanceService.class);
            var instance = new ProcessInstanceEntity().setId(7L).setProcessId(1).setInitialNodeId(11)
                    .setInitialPayload(Map.of("input", "test")).setStatus(ProcessInstanceStatus.Failed);
            when(instances.retrieve(7L)).thenReturn(Optional.of(instance));
            when(instances.save(instance)).thenReturn(instance);
            controller = new ProcessInstanceController(audit, users, instances, tasks, rabbit, loggers, permissions,
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
