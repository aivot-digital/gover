package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.services.ProcessInstanceTaskService;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.workers.ProcessWorker;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessInstanceTaskControllerTest {
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserService userService;
    @Mock
    private ProcessInstanceTaskService processInstanceTaskService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private ProcessService processService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private PermissionService permissionService;

    private ProcessInstanceTaskController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new ProcessInstanceTaskController(
                new AuditService(auditLogService),
                userService,
                processInstanceTaskService,
                departmentService,
                processService,
                rabbitTemplate,
                permissionService
        );

        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    @Test
    void rerunFailedTask_ShouldMarkTaskAsRestartedAndQueueWork() throws Exception {
        var task = createTask(ProcessTaskStatus.Failed);
        var user = mock(UserEntity.class);

        when(processInstanceTaskService.retrieve(task.getId())).thenReturn(Optional.of(task));
        when(processInstanceTaskService.save(task)).thenReturn(task);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(user.asSuperAdmin()).thenReturn(Optional.of(user));

        var result = controller.rerunFailedTask(jwt, task.getId());

        assertEquals(ProcessTaskStatus.Restarted, result.getStatus());
        assertNotNull(result.getUpdated());

        var payloadCaptor = ArgumentCaptor.forClass(ProcessWorker.WorkerPayload.class);
        verify(processInstanceTaskService).save(task);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE),
                payloadCaptor.capture()
        );

        var payload = payloadCaptor.getValue();
        assertEquals(task.getProcessInstanceId(), payload.processInstanceId());
        assertEquals(task.getPreviousProcessNodeId(), payload.previousNodeId());
        assertEquals(task.getProcessNodeId(), payload.nextNodeId());
    }

    @Test
    void rerunFailedTask_ShouldRejectNonFailedTasks() throws Exception {
        var task = createTask(ProcessTaskStatus.Restarted);

        when(processInstanceTaskService.retrieve(task.getId())).thenReturn(Optional.of(task));

        var exception = assertThrows(ResponseException.class, () -> controller.rerunFailedTask(jwt, task.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(processInstanceTaskService, never()).save(task);
        verifyNoInteractions(rabbitTemplate);
    }

    private ProcessInstanceTaskEntity createTask(ProcessTaskStatus status) {
        return new ProcessInstanceTaskEntity()
                .setId(7L)
                .setAccessKey(UUID.randomUUID())
                .setProcessInstanceId(13L)
                .setProcessId(21)
                .setProcessVersion(1)
                .setProcessNodeId(34)
                .setPreviousProcessNodeId(33)
                .setStatus(status)
                .setStarted(LocalDateTime.now().minusMinutes(5))
                .setUpdated(LocalDateTime.now().minusMinutes(1))
                .setFinished(LocalDateTime.now())
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
                .setProcessData(Map.of());
    }
}
