package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessAccessControlEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessAccessControlService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAccessControlControllerTest {
    private static final String UPDATE = ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE;
    private static final int RULE_ID = 5;
    private static final int PROCESS_ID = 17;
    private static final int MANAGING_DEPARTMENT_ID = 3;

    private final ProcessAccessControlService accessControlService = mock(ProcessAccessControlService.class);
    private final ProcessService processService = mock(ProcessService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserEntity user = mock(UserEntity.class);
    private final ProcessAccessControlEntity rule = new ProcessAccessControlEntity().setId(RULE_ID).setTargetProcessId(PROCESS_ID);

    private ProcessAccessControlController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        var process = mock(ProcessEntity.class);
        when(process.getDepartmentId()).thenReturn(MANAGING_DEPARTMENT_ID);
        when(user.getId()).thenReturn("user-1");
        when(processService.retrieve(PROCESS_ID)).thenReturn(Optional.of(process));
        when(accessControlService.retrieve(RULE_ID)).thenReturn(Optional.of(rule));
        controller = new ProcessAccessControlController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class),
                accessControlService,
                permissionService,
                processService
        );
    }

    @Test
    void managingDepartmentMayCreateUpdateAndDeleteRules() throws ResponseException {
        controller.checkCreatePermissions(user, rule);
        controller.checkUpdatePermission(user, RULE_ID);
        controller.checkDeletePermission(user, RULE_ID);

        verify(permissionService, times(3))
                .requireDepartmentPermission("user-1", MANAGING_DEPARTMENT_ID, UPDATE);
        verify(permissionService, never()).requireProcessPermission("user-1", PROCESS_ID, UPDATE);
    }

    @Test
    void delegatedProcessUpdateDoesNotAllowChangingRules() throws ResponseException {
        // A delegated OE or team has the process grant, but not the permission on the managing department.
        doThrow(ResponseException.forbidden())
                .when(permissionService).requireDepartmentPermission("user-1", MANAGING_DEPARTMENT_ID, UPDATE);

        assertForbidden(() -> controller.checkCreatePermissions(user, rule));
        assertForbidden(() -> controller.checkUpdatePermission(user, RULE_ID));
        assertForbidden(() -> controller.checkDeletePermission(user, RULE_ID));
    }

    @Test
    void delegatedProcessUpdateMayStillReadRules() throws ResponseException {
        doThrow(ResponseException.forbidden())
                .when(permissionService).requireDepartmentPermission("user-1", MANAGING_DEPARTMENT_ID, UPDATE);

        assertDoesNotThrow(() -> controller.checkRetrievePermissions(user, RULE_ID));

        verify(permissionService).requireProcessPermission("user-1", PROCESS_ID, UPDATE);
    }

    private static void assertForbidden(Executable executable) {
        assertThrows(ResponseException.class, executable);
    }
}
