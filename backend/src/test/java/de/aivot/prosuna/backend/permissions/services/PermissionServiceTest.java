package de.aivot.prosuna.backend.permissions.services;

import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import org.junit.jupiter.api.Test;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK;
import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PermissionServiceTest {
    private final VUserSystemPermissionRepository systemPermissions = mock(VUserSystemPermissionRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final PermissionService service = new PermissionService(null, null, systemPermissions, null, null, null, instances);

    @Test
    void ownSystemPermissionOverridesInstanceScopeForLastingAssignments() {
        when(systemPermissions.hasPermissionWithoutDeputies("user", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(service.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertTrue(service.hasProcessInstancePermissionWithoutDeputies("user", 18L, PROCESS_INSTANCE_EDIT_TASK));
        verifyNoInteractions(instances);
    }

    @Test
    void ownScopedPermissionOnlyAuthorizesItsUserInstanceAndKey() {
        when(instances.hasPermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(service.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies("user", 18L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies("other", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_READ));
    }

    @Test
    void deputySystemPermissionAllowsAccessButNotLastingAssignments() {
        when(systemPermissions.hasPermission("user", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(service.hasProcessInstancePermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
    }

    @Test
    void deputyScopedPermissionAllowsAccessButNotLastingAssignments() {
        when(instances.hasPermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(service.hasProcessInstancePermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
    }

    @Test
    void missingUserHasNoLastingAssignmentPermissions() {
        assertFalse(service.hasProcessInstancePermissionWithoutDeputies(null, 17L, PROCESS_INSTANCE_EDIT_TASK));
        verifyNoInteractions(systemPermissions, instances);
    }
}
