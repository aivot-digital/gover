package de.aivot.prosuna.backend.permissions.services;

import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK;
import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionServiceTest {
    private static final String USER_ID = "user-1";
    private static final String PERMISSION = ProcessPermissionProvider.PROCESS_DEFINITION_READ;

    private ProcessRepository processRepository;
    private ProcessInstanceRepository instances;
    private VUserSystemPermissionRepository systemPermissionRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        processRepository = mock(ProcessRepository.class);
        instances = mock(ProcessInstanceRepository.class);
        systemPermissionRepository = mock(VUserSystemPermissionRepository.class);
        permissionService = new PermissionService(
                mock(VUserDepartmentPermissionRepository.class),
                mock(VUserTeamPermissionRepository.class),
                systemPermissionRepository,
                mock(DepartmentRepository.class),
                mock(TeamRepository.class),
                processRepository,
                instances
        );
    }

    @Test
    void requireProcessPermissionShouldRespectTheGrantedProcess() {
        when(processRepository.hasPermission(USER_ID, 42, PERMISSION)).thenReturn(true);

        assertDoesNotThrow(() -> permissionService.requireProcessPermission(USER_ID, 42, PERMISSION));

        var error = assertThrows(ResponseException.class,
                () -> permissionService.requireProcessPermission(USER_ID, 43, PERMISSION));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void requireProcessPermissionShouldAcceptTheSystemPermission() {
        when(systemPermissionRepository.hasPermission(USER_ID, PERMISSION)).thenReturn(true);

        assertDoesNotThrow(() -> permissionService.requireProcessPermission(USER_ID, 42, PERMISSION));
        assertDoesNotThrow(() -> permissionService.requireProcessPermission(USER_ID, 43, PERMISSION));
    }

    @Test
    void ownSystemPermissionOverridesInstanceScopeForLastingAssignments() {
        when(systemPermissionRepository.hasPermissionWithoutDeputies("user", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertTrue(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 18L, PROCESS_INSTANCE_EDIT_TASK));
        verifyNoInteractions(instances);
    }

    @Test
    void ownScopedPermissionOnlyAuthorizesItsUserInstanceAndKey() {
        when(instances.hasPermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 18L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies("other", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_READ));
    }

    @Test
    void deputySystemPermissionAllowsAccessButNotLastingAssignments() {
        when(systemPermissionRepository.hasPermission("user", PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(permissionService.hasProcessInstancePermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
    }

    @Test
    void deputyScopedPermissionAllowsAccessButNotLastingAssignments() {
        when(instances.hasPermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK)).thenReturn(true);

        assertTrue(permissionService.hasProcessInstancePermission("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies("user", 17L, PROCESS_INSTANCE_EDIT_TASK));
    }

    @Test
    void missingUserHasNoLastingAssignmentPermissions() {
        assertFalse(permissionService.hasProcessInstancePermissionWithoutDeputies(null, 17L, PROCESS_INSTANCE_EDIT_TASK));
        verifyNoInteractions(systemPermissionRepository, instances);
    }
}
