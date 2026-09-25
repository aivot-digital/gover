package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.projections.ProcessTaskAssignmentProjection;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.util.*;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceAccessGuardTest {
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final VUserSystemPermissionRepository system = mock(VUserSystemPermissionRepository.class);
    private final PermissionService permissions = new PermissionService(null, null, system, null, null, null, instances);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ProcessInstanceAccessGuard guard = new ProcessInstanceAccessGuard(instances, tasks, permissions,
            nodes, mock(ProcessNodeDefinitionService.class), users);
    private final ProcessInstanceTaskEntity task = task(1L, "alice", ProcessTaskStatus.Running);

    @BeforeEach
    void setup() {
        when(instances.lockAccessById(17L)).thenReturn(Optional.of(17L));
        when(tasks.findAssignmentSnapshots(17L)).thenAnswer(ignored -> List.of(snapshot(task)));
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_READ)).thenReturn(true);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        when(nodes.findById(5)).thenReturn(Optional.of(new ProcessNodeEntity().setName("Prüfung")));
        when(users.findById("alice")).thenReturn(Optional.of(new UserEntity().setId("alice").setFullName("Alice Beispiel")));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Running", "Paused", "AwaitingCustomer", "AwaitingPayment"})
    void rejectsLostEditAccessOnEveryActiveStatus(ProcessTaskStatus status) throws Exception {
        task.setStatus(status);
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(false);
        var error = assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before, true));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertTrue(error.getMessage().contains("anderen berechtigten Personen"));
        var details = (Map<?, ?>) error.getDetails();
        assertEquals("assigned_tasks_lose_access", details.get("reason"));
        assertEquals(List.of(new ProcessInstanceAccessGuard.AffectedTask(1L, "Prüfung", "Alice Beispiel")), details.get("tasks"));
    }

    @Test
    void rejectsLossOfReadAccessEvenIfTaskEditRemains() throws Exception {
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before, true));
    }

    @Test
    void acceptsRemainingScopedAndSystemGrants() throws Exception {
        var before = guard.lockAndSnapshot(17L);
        guard.requireRetainedAccess(17L, before, true);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(false);
        when(system.hasPermissionWithoutDeputies("alice", PROCESS_INSTANCE_READ)).thenReturn(true);
        when(system.hasPermissionWithoutDeputies("alice", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        guard.requireRetainedAccess(17L, before, true);
        verifyNoInteractions(users, nodes);
    }

    @Test
    void doesNotAcceptAGrantOnAnotherInstance() throws Exception {
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(false);
        when(instances.hasPermissionWithoutDeputies("alice", 18L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsLosingOwnAccessEvenIfDeputyAccessRemains(boolean systemAccess) throws Exception {
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(false);
        if (systemAccess) when(system.hasPermission("alice", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        else when(instances.hasPermission("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        var error = assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("assigned_tasks_lose_access", ((Map<?, ?>) error.getDetails()).get("reason"));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Completed", "Aborted", "Failed", "Restarted"})
    void ignoresFinishedTasks(ProcessTaskStatus status) throws Exception {
        task.setStatus(status);
        assertTrue(guard.lockAndSnapshot(17L).isEmpty());
    }

    @Test
    void ignoresUnassignedTasksAndPreviouslyMissingPermissions() throws Exception {
        task.setAssignedUserId(null);
        assertTrue(guard.lockAndSnapshot(17L).isEmpty());
        task.setAssignedUserId("alice");
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(false);
        guard.requireRetainedAccess(17L, guard.lockAndSnapshot(17L));
    }

    @Test
    void reportsAllAffectedTasksButNotOtherRecipients() throws Exception {
        when(tasks.findAssignmentSnapshots(17L)).thenReturn(List.of(snapshot(task),
                snapshot(task(2L, "alice", ProcessTaskStatus.Paused)), snapshot(task(3L, "bob", ProcessTaskStatus.Running))));
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        var error = assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before, true));
        var affected = (List<?>) ((Map<?, ?>) error.getDetails()).get("tasks");
        assertEquals(2, affected.size());
    }

    @Test
    void doesNotExposeTaskDetailsWithoutReadPermission() throws Exception {
        var before = guard.lockAndSnapshot(17L);
        when(instances.hasPermissionWithoutDeputies("alice", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        var error = assertThrows(ResponseException.class, () -> guard.requireRetainedAccess(17L, before));
        assertEquals(List.of(), ((Map<?, ?>) error.getDetails()).get("tasks"));
        verifyNoInteractions(users, nodes);
    }

    private ProcessTaskAssignmentProjection snapshot(ProcessInstanceTaskEntity value) {
        return new ProcessTaskAssignmentProjection(value.getId(), value.getProcessNodeId(), value.getStatus(), value.getAssignedUserId());
    }

    private ProcessInstanceTaskEntity task(Long id, String userId, ProcessTaskStatus status) {
        return new ProcessInstanceTaskEntity().setId(id).setProcessInstanceId(17L).setProcessNodeId(5).setStatus(status).setAssignedUserId(userId);
    }
}
