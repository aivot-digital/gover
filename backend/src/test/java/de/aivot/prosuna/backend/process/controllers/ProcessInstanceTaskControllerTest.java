package de.aivot.prosuna.backend.process.controllers;

import static de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport.passthrough;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.controllers.ProcessInstanceTaskController;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceRestartService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessInstanceTaskControllerTest {
    @Mock
    private UserService userService;
    @Mock
    private ProcessInstanceTaskService processInstanceTaskService;
    @Mock
    private ProcessInstanceRestartService restartService;
    @Mock
    private PermissionService permissionService;

    @Mock
    private de.aivot.prosuna.backend.process.services.ProcessListService listService;

    private ProcessInstanceTaskController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new ProcessInstanceTaskController(
                userService,
                processInstanceTaskService,
                restartService,
                permissionService, mock(de.aivot.prosuna.backend.process.services.ProcessAssignmentService.class),
                listService,
                passthrough()
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
    void assignedCountUsesPermissionScopedOpenTaskQuery() throws Exception {
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");
        when(userService.fromJWT(jwt)).thenReturn(java.util.Optional.of(user));
        when(listService.countOpenAssignedTasks("user-1")).thenReturn(4L);
        org.junit.jupiter.api.Assertions.assertEquals(Map.of("count", 4L), controller.countAssignedTasks(jwt));
        verifyNoInteractions(processInstanceTaskService);
    }

    @Test
    void assignedCountRequiresAuthentication() throws Exception {
        when(userService.fromJWT(jwt)).thenReturn(java.util.Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(ResponseException.class, () -> controller.countAssignedTasks(jwt));
        verifyNoInteractions(listService);
    }

    @Test
    void rerunFailedTask_ShouldDelegateTheAuthorizationToTheRestartService() throws Exception {
        var task = createTask(ProcessTaskStatus.Failed);
        var restarted = createTask(ProcessTaskStatus.Restarted);
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");

        when(processInstanceTaskService.retrieve(task.getId())).thenReturn(Optional.of(task));
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        when(restartService.rerunFailedTask(eq(task.getProcessInstanceId()), eq(task.getId()), eq(user), any()))
                .thenReturn(restarted);

        var result = controller.rerunFailedTask(jwt, task.getId());

        assertSame(restarted, result);
        // The restart service runs the authorization under the instance lock, so the controller must not check first.
        verifyNoInteractions(permissionService);
        var authorization = ArgumentCaptor.forClass(ProcessTaskExecutionService.Authorization.class);
        verify(restartService).rerunFailedTask(eq(task.getProcessInstanceId()), eq(task.getId()), eq(user),
                authorization.capture());
        authorization.getValue().run();
        verify(permissionService).requireProcessInstancePermission("user-1", task.getProcessInstanceId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK);
    }

    @Test
    void rerunFailedTask_ShouldRejectTheRerunWithoutPermission() throws Exception {
        var task = createTask(ProcessTaskStatus.Failed);
        var user = mock(UserEntity.class);
        when(user.getId()).thenReturn("user-1");
        when(processInstanceTaskService.retrieve(task.getId())).thenReturn(Optional.of(task));
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        doThrow(ResponseException.forbidden()).when(permissionService).requireProcessInstancePermission("user-1",
                task.getProcessInstanceId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK);
        when(restartService.rerunFailedTask(eq(task.getProcessInstanceId()), eq(task.getId()), eq(user), any()))
                .thenAnswer(invocation -> {
                    invocation.<ProcessTaskExecutionService.Authorization>getArgument(3).run();
                    return createTask(ProcessTaskStatus.Restarted);
                });

        var exception = assertThrows(ResponseException.class, () -> controller.rerunFailedTask(jwt, task.getId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void rerunFailedTask_ShouldRejectNonFailedTasks() throws Exception {
        var task = createTask(ProcessTaskStatus.Restarted);

        when(processInstanceTaskService.retrieve(task.getId())).thenReturn(Optional.of(task));

        var exception = assertThrows(ResponseException.class, () -> controller.rerunFailedTask(jwt, task.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(restartService);
    }

    private ProcessInstanceTaskEntity createTask(ProcessTaskStatus status) {
        return new ProcessInstanceTaskEntity()
                .setId(7L)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessInstanceId(13L)
                .setProcessId(21)
                .setProcessVersion(1)
                .setProcessNodeId(34)
                .setPreviousProcessInstanceTaskId(6L)
                .setPreviousProcessNodeId(33)
                .setPreviousProcessNodePortKey("approved")
                .setStatus(status)
                .setStarted(Instant.now().minusSeconds(5 * 60L))
                .setUpdated(Instant.now().minusSeconds(60))
                .setFinished(Instant.now())
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
                .setProcessData(Map.of());
    }
}
