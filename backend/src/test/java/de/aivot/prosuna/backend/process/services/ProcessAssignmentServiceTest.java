package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.audit.models.AuditLogPayload;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessAssignmentServiceTest {
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final VUserSystemPermissionRepository systemPermissions = mock(VUserSystemPermissionRepository.class);
    private final PermissionService permissions = new PermissionService(null, null, systemPermissions, null, null, null, instances);
    private final AuditService auditService = mock(AuditService.class);
    private final ScopedAuditService audit = mock(ScopedAuditService.class);
    private ProcessAssignmentService service;
    private final UserEntity actor = user("actor");
    private final UserEntity recipient = user("recipient");
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(17L).setProcessId(2).setAssignedUserId("previous");
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(5L).setProcessInstanceId(17L)
            .setStatus(ProcessTaskStatus.Running).setAssignedUserId("previous");

    @BeforeEach
    void setup() {
        when(auditService.createScopedAuditService(any(), anyString())).thenReturn(audit);
        when(audit.create()).thenAnswer(invocation -> AuditLogPayload.create(audit));
        service = new ProcessAssignmentService(permissions, users, instances, tasks, auditService);
        when(instances.lockAccessById(17L)).thenReturn(Optional.of(17L));
        when(tasks.findInstanceIdById(5L)).thenReturn(Optional.of(17L));
        when(instances.findById(17L)).thenReturn(Optional.of(instance));
        when(instances.existsById(17L)).thenReturn(true);
        when(tasks.findById(5L)).thenReturn(Optional.of(task));
        when(users.findById("recipient")).thenReturn(Optional.of(recipient));
        when(instances.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assignsWithinScopeAndAuditsOldAndNewAssignee() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("recipient", PROCESS_INSTANCE_READ);
        var result = service.reassignInstance(actor, 17L, "recipient");
        assertEquals("recipient", result.getAssignedUserId());
        assertNotNull(result.getUpdated());
        assertEquals("previous", task.getAssignedUserId());
        verify(instances).saveAndFlush(instance);
        var payload = ArgumentCaptor.forClass(AuditLogPayload.class);
        verify(audit).addAuditEntry(payload.capture());
        assertNotNull(payload.getValue().getDiff());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reassignsTasksWithScopedOrSystemPermissions(boolean systemAccess) throws Exception {
        if (systemAccess) {
            when(systemPermissions.hasPermission("actor", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
            when(systemPermissions.hasPermissionWithoutDeputies("recipient", PROCESS_INSTANCE_READ)).thenReturn(true);
            when(systemPermissions.hasPermissionWithoutDeputies("recipient", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        } else {
            grant("actor", PROCESS_INSTANCE_EDIT_TASK);
            grant("recipient", PROCESS_INSTANCE_READ);
            grant("recipient", PROCESS_INSTANCE_EDIT_TASK);
        }
        var result = service.reassignTask(actor, 5L, "recipient");
        assertEquals("recipient", result.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Running, result.getStatus());
        assertNull(result.getFinished());
        assertEquals("previous", instance.getAssignedUserId());
        verify(audit).addAuditEntry(any());
    }

    @Test
    void rejectsActorWithPermissionOnlyOnAnotherInstance() throws Exception {
        when(instances.hasPermission("actor", 18L, PROCESS_INSTANCE_REASSIGN)).thenReturn(true);
        when(instances.hasPermission("actor", 18L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        assertThrows(ResponseException.class, () -> service.instanceOptions("actor", 17L));
        assertThrows(ResponseException.class, () -> service.taskOptions("actor", 5L));
        verifyNoInteractions(users, audit);
        verify(instances, never()).saveAndFlush(any());
        verify(tasks, never()).saveAndFlush(any());
    }

    @Test
    void separatesReassignmentAndTaskEditPermissions() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        verify(tasks, never()).saveAndFlush(any());
    }

    @Test
    void checksRecipientsOnTheActualInstanceAtSaveTime() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        when(instances.hasPermissionWithoutDeputies("recipient", 18L, PROCESS_INSTANCE_READ)).thenReturn(true);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        grant("recipient", PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        verifyNoInteractions(audit);
        verify(instances, never()).saveAndFlush(any());
        verify(tasks, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveDeletedAndMissingRecipients() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("recipient", PROCESS_INSTANCE_READ);
        recipient.setEnabled(false);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        recipient.setEnabled(true).setDeletedInIdp(true);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "missing"));
        verifyNoInteractions(audit);
    }

    @Test
    void clearsInstanceAssignmentEvenIfFormerRecipientIsUnavailable() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        assertNull(service.reassignInstance(actor, 17L, null).getAssignedUserId());
        assertEquals("previous", task.getAssignedUserId());
        verifyNoInteractions(users);
        verify(audit).addAuditEntry(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsClearingTaskAssignmentsEvenWithSystemPermission(boolean systemAccess) {
        if (systemAccess) when(systemPermissions.hasPermission("actor", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        else grant("actor", PROCESS_INSTANCE_EDIT_TASK);

        var exception = assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die Zuweisung einer Aufgabe kann nicht aufgehoben werden. Bitte wählen Sie eine andere Person aus.", exception.getMessage());
        assertEquals("previous", task.getAssignedUserId());
        assertNull(task.getUpdated());
        verify(tasks, never()).saveAndFlush(any());
        verifyNoInteractions(audit, users);
    }

    @Test
    void listsOnlyEligibleRecipientsIncludingGlobalGrantsAndRechecksWhenSaving() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        grant("recipient", PROCESS_INSTANCE_READ);
        var global = user("global");
        var otherScope = user("other-scope");
        when(systemPermissions.hasPermissionWithoutDeputies("global", PROCESS_INSTANCE_READ)).thenReturn(true);
        when(systemPermissions.hasPermissionWithoutDeputies("global", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        when(instances.hasPermissionWithoutDeputies("other-scope", 18L, PROCESS_INSTANCE_READ)).thenReturn(true);
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc()).thenReturn(List.of(recipient, global, otherScope));
        assertEquals(List.of("recipient", "global"), service.instanceOptions("actor", 17L).stream().map(option -> option.id()).toList());
        assertEquals(List.of("global"), service.taskOptions("actor", 5L).stream().map(option -> option.id()).toList());
        when(instances.hasPermissionWithoutDeputies("recipient", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        verifyNoInteractions(audit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void excludesDeputyOnlyRecipientsFromOptionsAndAllAssignmentPaths(boolean systemAccess) throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        for (var key : List.of(PROCESS_INSTANCE_READ, PROCESS_INSTANCE_EDIT_TASK)) {
            if (systemAccess) when(systemPermissions.hasPermission("recipient", key)).thenReturn(true);
            else when(instances.hasPermission("recipient", 17L, key)).thenReturn(true);
        }
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc()).thenReturn(List.of(recipient));

        assertTrue(service.instanceOptions("actor", 17L).isEmpty());
        assertTrue(service.taskOptions("actor", 5L).isEmpty());
        var error = assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getMessage().contains("Stellvertretung"));
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        assertThrows(ResponseException.class, () -> service.saveRuntimeAssignment(task, "recipient"));
        assertEquals("previous", instance.getAssignedUserId());
        assertEquals("previous", task.getAssignedUserId());
        verify(instances, never()).saveAndFlush(any());
        verify(tasks, never()).saveAndFlush(any());
        verifyNoInteractions(audit);
    }

    @ParameterizedTest
    @ValueSource(strings = {PROCESS_INSTANCE_READ, PROCESS_INSTANCE_EDIT_TASK})
    void requiresEachTaskPermissionIndependentlyOfDeputies(String deputyOnlyPermission) throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        grant("recipient", PROCESS_INSTANCE_READ);
        grant("recipient", PROCESS_INSTANCE_EDIT_TASK);
        when(instances.hasPermissionWithoutDeputies("recipient", 17L, deputyOnlyPermission)).thenReturn(false);
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc()).thenReturn(List.of(recipient));

        assertEquals(PROCESS_INSTANCE_READ.equals(deputyOnlyPermission) ? 0 : 1,
                service.instanceOptions("actor", 17L).size());
        assertTrue(service.taskOptions("actor", 5L).isEmpty());
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        verify(tasks, never()).saveAndFlush(any());
        verifyNoInteractions(audit);
    }

    @Test
    void allowsDeputiesToAssignTasksToEligibleRecipients() throws Exception {
        when(instances.hasPermission("actor", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        grant("recipient", PROCESS_INSTANCE_READ);
        grant("recipient", PROCESS_INSTANCE_EDIT_TASK);

        assertEquals("recipient", service.reassignTask(actor, 5L, "recipient").getAssignedUserId());
        verify(audit).addAuditEntry(any());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Completed", "Aborted", "Failed", "Restarted"})
    void preservesAssignmentsOnFinishedTasks(ProcessTaskStatus status) throws Exception {
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        task.setStatus(status);
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, "recipient"));
        assertThrows(ResponseException.class, () -> service.taskOptions("actor", 5L));
        verify(tasks, never()).saveAndFlush(any());
        verifyNoInteractions(audit, users);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void automaticEligibilityRequiresBothOwnTaskPermissionsInTheActualScope(boolean systemAccess) {
        for (var key : List.of(PROCESS_INSTANCE_READ, PROCESS_INSTANCE_EDIT_TASK)) {
            when(instances.hasPermissionWithoutDeputies("recipient", 18L, key)).thenReturn(true);
            when(instances.hasPermission("recipient", 17L, key)).thenReturn(true);
        }
        assertFalse(service.canReceiveTaskAssignment("recipient", 17L, List.of(PROCESS_INSTANCE_EDIT_TASK)));
        if (systemAccess) {
            when(systemPermissions.hasPermissionWithoutDeputies("recipient", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        } else {
            grant("recipient", PROCESS_INSTANCE_EDIT_TASK);
        }
        assertFalse(service.canReceiveTaskAssignment("recipient", 17L, List.of(PROCESS_INSTANCE_EDIT_TASK)));
        if (systemAccess) {
            when(systemPermissions.hasPermissionWithoutDeputies("recipient", PROCESS_INSTANCE_READ)).thenReturn(true);
        } else {
            grant("recipient", PROCESS_INSTANCE_READ);
        }
        assertTrue(service.canReceiveTaskAssignment("recipient", 17L, List.of(PROCESS_INSTANCE_EDIT_TASK)));
        assertFalse(service.canReceiveTaskAssignment("recipient", 17L, List.of(PROCESS_INSTANCE_UPDATE)));
        recipient.setEnabled(false);
        assertFalse(service.canReceiveTaskAssignment("recipient", 17L, List.of()));
        recipient.setEnabled(true).setDeletedInIdp(true);
        assertFalse(service.canReceiveTaskAssignment("recipient", 17L, List.of()));
        assertFalse(service.canReceiveTaskAssignment("missing", 17L, List.of()));
    }

    @Test
    void runtimeAssignmentsUseInstanceLockAndCheckCurrentRights() throws Exception {
        grant("recipient", PROCESS_INSTANCE_READ);
        grant("recipient", PROCESS_INSTANCE_EDIT_TASK);
        service.saveRuntimeAssignment(task, "recipient");
        assertEquals("recipient", task.getAssignedUserId());
        var order = inOrder(instances, tasks);
        order.verify(instances).lockAccessById(17L);
        order.verify(instances).hasPermissionWithoutDeputies("recipient", 17L, PROCESS_INSTANCE_READ);
        order.verify(instances).hasPermissionWithoutDeputies("recipient", 17L, PROCESS_INSTANCE_EDIT_TASK);
        order.verify(tasks).saveAndFlush(task);
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsRuntimeAssignmentAfterPermissionWithdrawal() throws Exception {
        grant("recipient", PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> service.saveRuntimeAssignment(task, "recipient"));
        assertEquals("previous", task.getAssignedUserId());
        verify(tasks, never()).saveAndFlush(any());
        verify(instances).lockAccessById(17L);
    }

    private void grant(String userId, String permission) {
        when(instances.hasPermission(userId, 17L, permission)).thenReturn(true);
        when(instances.hasPermissionWithoutDeputies(userId, 17L, permission)).thenReturn(true);
    }

    private static UserEntity user(String id) {
        return new UserEntity().setId(id).setFullName(id).setEnabled(true).setDeletedInIdp(false);
    }
}
