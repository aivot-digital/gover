package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.models.AuditLogPayload;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceRestartServiceTest {
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessWorkRequestRepository requests = mock(ProcessWorkRequestRepository.class);
    private final ProcessWorkRequestService workRequests = mock(ProcessWorkRequestService.class);
    private final ProcessTaskExecutionService execution = ProcessTaskExecutionTestSupport.passthrough();
    private final ProcessNodeExecutionLoggerFactory loggers = mock(ProcessNodeExecutionLoggerFactory.class);
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(7L).setProcessId(1).setInitialNodeId(11)
            .setInitialPayload(Map.of("input", "test")).setStatus(ProcessInstanceStatus.Failed);
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(34L).setProcessInstanceId(7L)
            .setProcessNodeId(56).setStatus(ProcessTaskStatus.Failed);
    private final AuditService auditService = mock(AuditService.class);
    private final ScopedAuditService audit = mock(ScopedAuditService.class);
    private final UserEntity actor = new UserEntity().setId("actor").setFullName("Erika Muster").setEnabled(true)
            .setDeletedInIdp(false);
    private ProcessInstanceRestartService service;

    @BeforeEach
    void setup() {
        when(auditService.createScopedAuditService(ProcessInstanceRestartService.class, "Vorgänge")).thenReturn(audit);
        when(audit.create()).thenAnswer(invocation -> AuditLogPayload.create(audit));
        service = new ProcessInstanceRestartService(instances, tasks, requests, workRequests, execution, loggers,
                entityManager, auditService);
        when(instances.findById(7L)).thenReturn(Optional.of(instance));
        when(instances.save(instance)).thenReturn(instance);
        when(tasks.findById(34L)).thenReturn(Optional.of(task));
        when(tasks.save(task)).thenReturn(task);
        when(loggers.create(any(), any(), any(), any())).thenReturn(logger);
    }

    @Test
    void restartRunsTheFailedLatestTaskAgain() throws Exception {
        when(tasks.findFirstByProcessInstanceIdOrderByStartedDescIdDesc(7L)).thenReturn(Optional.of(task));

        var result = service.restartFailedInstance(7L, "actor", () -> {});

        assertSame(instance, result);
        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());
        assertEquals(ProcessTaskStatus.Restarted, task.getStatus());
        verify(execution).inInstance(eq(7L), any());
        verify(tasks).save(task);
        verify(workRequests).enqueueRestart(7L, 34L);
        verify(workRequests, never()).enqueueStart(any());
        verify(loggers).create(7L, 34L, "actor", null);
        verify(logger).logf(ProcessNodeExecutionLogLevel.Info, true, true, "Fortsetzung des Vorgangs angefordert",
                "Die Fortsetzung des fehlgeschlagenen Vorgangs wurde manuell angefordert. Die letzte fehlgeschlagene Aufgabe wird erneut ausgeführt.");
    }

    @Test
    void restartStartsAnInstanceWithoutTasksAgain() throws Exception {
        service.restartFailedInstance(7L, "actor", () -> {});

        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());
        verify(workRequests).enqueueStart(7L);
        verify(workRequests, never()).enqueueRestart(any(), any());
        verify(loggers).create(7L, null, "actor", null);
    }

    @Test
    void restartWithoutTasksRequiresTheInitialData() {
        instance.setInitialPayload(Map.of());

        var exception = assertThrows(ResponseException.class, () -> service.restartFailedInstance(7L, "actor", () -> {}));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        verifyNoInteractions(workRequests, loggers);
        verify(instances, never()).save(any());
    }

    @Test
    void restartReleasesHeldWorkWhenTheLatestTaskDidNotFail() throws Exception {
        task.setStatus(ProcessTaskStatus.Completed);
        when(tasks.findFirstByProcessInstanceIdOrderByStartedDescIdDesc(7L)).thenReturn(Optional.of(task));
        when(requests.findIdsByProcessInstanceId(7L)).thenReturn(List.of(3L));

        service.restartFailedInstance(7L, "actor", () -> {});

        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verify(workRequests).redispatchAfterCommit(List.of(3L));
        verify(workRequests, never()).enqueueRestart(any(), any());
        verify(logger).logf(ProcessNodeExecutionLogLevel.Info, true, true, "Fortsetzung des Vorgangs angefordert",
                "Die Fortsetzung des fehlgeschlagenen Vorgangs wurde manuell angefordert. Die ausstehenden Verarbeitungsschritte werden erneut ausgeführt.");
    }

    @Test
    void restartRejectsAnInstanceWithoutFailedTaskOrHeldWork() {
        task.setStatus(ProcessTaskStatus.Completed);
        when(tasks.findFirstByProcessInstanceIdOrderByStartedDescIdDesc(7L)).thenReturn(Optional.of(task));

        var exception = assertThrows(ResponseException.class, () -> service.restartFailedInstance(7L, "actor", () -> {}));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verifyNoInteractions(workRequests, loggers);
        verify(instances, never()).save(any());
    }

    @Test
    void concurrentRestartSeesTheFirstRestartAfterTheLock() throws Exception {
        // The first restart committed while this one waited for the instance lock.
        doAnswer(call -> instance.setStatus(ProcessInstanceStatus.Running)).when(entityManager).refresh(instance);

        var exception = assertThrows(ResponseException.class, () -> service.restartFailedInstance(7L, "actor", () -> {}));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(workRequests, loggers);
    }

    @Test
    void restartWithoutPermissionIsRejectedUnderTheLockAndChangesNothing() {
        var exception = assertThrows(ResponseException.class, () -> service.restartFailedInstance(7L, "actor", () -> {
            // The authorization runs within the instance lock, not before it.
            verify(execution).inInstance(eq(7L), any());
            throw ResponseException.forbidden();
        }));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        verify(instances, never()).save(any());
        verifyNoInteractions(workRequests, loggers);
    }

    @Test
    void rerunOfATaskOfAFailedInstanceRunsTheInstanceAndItsHeldWorkAgain() throws Exception {
        when(requests.findIdsByProcessInstanceId(7L)).thenReturn(List.of(3L));

        var result = service.rerunFailedTask(7L, 34L, actor, () -> {});

        assertSame(task, result);
        assertEquals(ProcessTaskStatus.Restarted, task.getStatus());
        assertNotNull(task.getUpdated());
        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());
        verify(execution).inInstance(eq(7L), any());
        verify(instances).save(instance);
        verify(workRequests).redispatchAfterCommit(List.of(3L));
        verify(workRequests).enqueueRestart(7L, 34L);
        verify(loggers).create(7L, 34L, "actor", null);
        verify(logger).logf(ProcessNodeExecutionLogLevel.Info, true, true, "Erneute Ausführung der Aufgabe angefordert",
                "Die erneute Ausführung der fehlgeschlagenen Aufgabe wurde manuell angefordert. Der fehlgeschlagene Vorgang wird fortgesetzt.");
        var payload = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(audit).addRequiredAuditEntry(payload.capture());
        assertEquals("actor", payload.getValue().getActorId());
        assertEquals("34", payload.getValue().getEntityRef());
        assertEquals(Map.of(
                "processInstanceStatus", Map.of("old", ProcessInstanceStatus.Failed, "new", ProcessInstanceStatus.Running),
                "status", Map.of("old", ProcessTaskStatus.Failed, "new", ProcessTaskStatus.Restarted)
        ), payload.getValue().getDiff());
        assertEquals("Die erneute Ausführung der fehlgeschlagenen Aufgabe mit der ID „34“ im Vorgang mit der ID „7“ wurde von der Mitarbeiter:in „Erika Muster“ angefordert.",
                payload.getValue().getMessage());
    }

    @Test
    void rerunInARunningInstanceOnlyRunsTheTaskAgain() throws Exception {
        instance.setStatus(ProcessInstanceStatus.Running);

        service.rerunFailedTask(7L, 34L, actor, () -> {});

        verify(instances, never()).save(any());
        verify(workRequests, never()).redispatchAfterCommit(any());
        verify(workRequests).enqueueRestart(7L, 34L);
        verify(logger).logf(ProcessNodeExecutionLogLevel.Info, true, true, "Erneute Ausführung der Aufgabe angefordert",
                "Die erneute Ausführung der fehlgeschlagenen Aufgabe wurde manuell angefordert.");
        var payload = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(audit).addRequiredAuditEntry(payload.capture());
        assertEquals(Map.of("status", Map.of("old", ProcessTaskStatus.Failed, "new", ProcessTaskStatus.Restarted)),
                payload.getValue().getDiff());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted", "Created", "Paused"})
    void rerunIsRejectedWhileTheInstanceCannotRun(ProcessInstanceStatus status) {
        instance.setStatus(status);

        var exception = assertThrows(ResponseException.class, () -> service.rerunFailedTask(7L, 34L, actor, () -> {}));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        verifyNoInteractions(workRequests, loggers, audit);
    }

    @Test
    void concurrentRerunSeesTheFirstRerunAfterTheLock() {
        // The first rerun committed while this one waited for the instance lock.
        doAnswer(call -> task.setStatus(ProcessTaskStatus.Restarted)).when(entityManager).refresh(task);

        var exception = assertThrows(ResponseException.class, () -> service.rerunFailedTask(7L, 34L, actor, () -> {}));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(workRequests);
        verify(tasks, never()).save(any());
    }

    @Test
    void rerunRejectsATaskOfAnotherInstance() {
        task.setProcessInstanceId(8L);

        var exception = assertThrows(ResponseException.class, () -> service.rerunFailedTask(7L, 34L, actor, () -> {}));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(workRequests);
    }

    @Test
    void rerunWithoutPermissionIsRejectedUnderTheLockAndChangesNothing() {
        var exception = assertThrows(ResponseException.class, () -> service.rerunFailedTask(7L, 34L, actor, () -> {
            // The authorization runs within the instance lock, not before it.
            verify(execution).inInstance(eq(7L), any());
            throw ResponseException.forbidden();
        }));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        verify(tasks, never()).save(any());
        verify(instances, never()).save(any());
        verifyNoInteractions(workRequests, loggers, audit);
    }
}
