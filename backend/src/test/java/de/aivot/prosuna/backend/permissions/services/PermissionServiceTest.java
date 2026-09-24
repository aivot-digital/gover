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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceTest {
    private static final String USER_ID = "user-1";
    private static final String PERMISSION = ProcessPermissionProvider.PROCESS_DEFINITION_READ;

    private ProcessRepository processRepository;
    private VUserSystemPermissionRepository systemPermissionRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        processRepository = mock(ProcessRepository.class);
        systemPermissionRepository = mock(VUserSystemPermissionRepository.class);
        permissionService = new PermissionService(
                mock(VUserDepartmentPermissionRepository.class),
                mock(VUserTeamPermissionRepository.class),
                systemPermissionRepository,
                mock(DepartmentRepository.class),
                mock(TeamRepository.class),
                processRepository,
                mock(ProcessInstanceRepository.class)
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
}
