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
import org.mockito.ArgumentCaptor;

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

    @Test
    void acceptsSystemOverridesForActorAndRecipient() throws Exception {
        when(systemPermissions.hasPermission("actor", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        when(systemPermissions.hasPermission("recipient", PROCESS_INSTANCE_READ)).thenReturn(true);
        when(systemPermissions.hasPermission("recipient", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
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
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, null));
        verify(tasks, never()).saveAndFlush(any());
    }

    @Test
    void checksRecipientsOnTheActualInstanceAtSaveTime() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        when(instances.hasPermission("recipient", 18L, PROCESS_INSTANCE_READ)).thenReturn(true);
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
    void clearsAssignmentEvenIfFormerRecipientIsUnavailable() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        assertNull(service.reassignInstance(actor, 17L, null).getAssignedUserId());
        assertNull(service.reassignTask(actor, 5L, null).getAssignedUserId());
        verifyNoInteractions(users);
        verify(audit, times(2)).addAuditEntry(any());
    }

    @Test
    void listsOnlyEligibleRecipientsIncludingGlobalGrantsAndRechecksWhenSaving() throws Exception {
        grant("actor", PROCESS_INSTANCE_REASSIGN);
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        grant("recipient", PROCESS_INSTANCE_READ);
        var global = user("global");
        var otherScope = user("other-scope");
        when(systemPermissions.hasPermission("global", PROCESS_INSTANCE_READ)).thenReturn(true);
        when(systemPermissions.hasPermission("global", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        when(instances.hasPermission("other-scope", 18L, PROCESS_INSTANCE_READ)).thenReturn(true);
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc()).thenReturn(List.of(recipient, global, otherScope));
        assertEquals(List.of("recipient", "global"), service.instanceOptions("actor", 17L).stream().map(option -> option.id()).toList());
        assertEquals(List.of("global"), service.taskOptions("actor", 5L).stream().map(option -> option.id()).toList());
        when(instances.hasPermission("recipient", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        assertThrows(ResponseException.class, () -> service.reassignInstance(actor, 17L, "recipient"));
        verifyNoInteractions(audit);
    }

    @Test
    void runtimeOptionsFilterCurrentRightsForAutomaticRecipientAndManualDispatcher() throws Exception {
        grant("recipient", PROCESS_INSTANCE_READ);
        grant("dispatcher", PROCESS_INSTANCE_READ);
        grant("dispatcher", PROCESS_INSTANCE_EDIT_TASK);
        grant("dispatcher", PROCESS_INSTANCE_REASSIGN);
        var dispatcher = user("dispatcher");
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc())
                .thenReturn(List.of(recipient, dispatcher));

        assertEquals(List.of("recipient", "dispatcher"), service.runtimeInstanceOptions(17L, List.of())
                .stream().map(option -> option.id()).toList());
        assertEquals(List.of("dispatcher"), service.runtimeInstanceOptions(17L,
                        List.of(PROCESS_INSTANCE_EDIT_TASK, PROCESS_INSTANCE_REASSIGN))
                .stream().map(option -> option.id()).toList());

        service.requireRuntimeInstanceAssignee(17L, "recipient");
        when(instances.hasPermission("recipient", 17L, PROCESS_INSTANCE_READ)).thenReturn(false);
        assertThrows(ResponseException.class, () -> service.requireRuntimeInstanceAssignee(17L, "recipient"));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Completed", "Aborted", "Failed", "Restarted"})
    void preservesAssignmentsOnFinishedTasks(ProcessTaskStatus status) throws Exception {
        grant("actor", PROCESS_INSTANCE_EDIT_TASK);
        task.setStatus(status);
        assertThrows(ResponseException.class, () -> service.reassignTask(actor, 5L, null));
        assertThrows(ResponseException.class, () -> service.taskOptions("actor", 5L));
        verify(tasks, never()).saveAndFlush(any());
        verifyNoInteractions(audit, users);
    }

    @Test
    void runtimeAssignmentsUseInstanceLockAndCheckCurrentRights() throws Exception {
        grant("recipient", PROCESS_INSTANCE_READ);
        grant("recipient", PROCESS_INSTANCE_EDIT_TASK);
        service.saveRuntimeAssignment(task, "recipient");
        assertEquals("recipient", task.getAssignedUserId());
        var order = inOrder(instances, tasks);
        order.verify(instances).lockAccessById(17L);
        order.verify(instances).hasPermission("recipient", 17L, PROCESS_INSTANCE_READ);
        order.verify(instances).hasPermission("recipient", 17L, PROCESS_INSTANCE_EDIT_TASK);
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
    }

    private static UserEntity user(String id) {
        return new UserEntity().setId(id).setFullName(id).setEnabled(true).setDeletedInIdp(false);
    }
}
