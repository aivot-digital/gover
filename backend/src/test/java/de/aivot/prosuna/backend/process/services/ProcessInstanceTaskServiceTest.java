package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK;
import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceTaskServiceTest {
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final VUserSystemPermissionRepository systemPermissions = mock(VUserSystemPermissionRepository.class);
    private final PermissionService permissions = new PermissionService(null, null, systemPermissions, null, null, null, instances);
    private final ProcessInstanceTaskService service = new ProcessInstanceTaskService(tasks, permissions);
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity()
            .setId(23L).setProcessInstanceId(17L).setStatus(ProcessTaskStatus.Running).setAssignedUserId("other");

    @BeforeEach
    void setup() {
        when(tasks.findById(23L)).thenReturn(Optional.of(task));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void allowsStaffViewWithMatchingScopedOrSystemEditPermission(boolean systemAccess) throws ResponseException {
        if (systemAccess) {
            when(systemPermissions.hasPermission("actor", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        } else {
            when(instances.hasPermission("actor", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        }

        assertSame(task, service.retrieveForStaffView("actor", 17L, 23L));
        verify(instances).hasPermission("actor", 17L, PROCESS_INSTANCE_EDIT_TASK);
        verify(tasks, never()).save(any());
    }

    @Test
    void rejectsPermissionForAnotherInstance() {
        when(instances.hasPermission("actor", 18L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 17L, 23L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(instances).hasPermission("actor", 17L, PROCESS_INSTANCE_EDIT_TASK);
    }

    @Test
    void rejectsReadPermissionWithoutEditPermission() {
        when(instances.hasPermission("actor", 17L, PROCESS_INSTANCE_READ)).thenReturn(true);

        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 17L, 23L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void assignmentDoesNotReplaceTheRequiredPermission() {
        task.setAssignedUserId("actor");

        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 17L, 23L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsMismatchedInstanceEvenWithEditPermission(boolean systemAccess) {
        when(instances.hasPermission("actor", 18L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);
        when(systemPermissions.hasPermission("actor", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(systemAccess);

        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 18L, 23L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(instances, systemPermissions);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = "Running", mode = EnumSource.Mode.EXCLUDE)
    void rejectsTasksThatAreNotRunning(ProcessTaskStatus status) {
        task.setStatus(status);
        when(instances.hasPermission("actor", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 17L, 23L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void rejectsMissingTask() {
        var error = assertThrows(ResponseException.class, () -> service.retrieveForStaffView("actor", 17L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verifyNoInteractions(instances, systemPermissions);
    }
}
