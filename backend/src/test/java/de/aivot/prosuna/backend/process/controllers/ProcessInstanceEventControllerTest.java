package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessHistoryEventScope;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.process.services.ProcessInstanceEventLogService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceEventService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProcessInstanceEventControllerTest {
    private final UserService users = mock(UserService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final ProcessInstanceEventLogService logs = mock(ProcessInstanceEventLogService.class);
    private final ProcessInstanceEventController controller = new ProcessInstanceEventController(
            users, mock(ProcessInstanceEventService.class), logs, permissions);

    @ParameterizedTest
    @ValueSource(strings = {"instance", "system", "other-instance"})
    void historyHonorsScopedGrantsAndTheSystemOverride(String grant) throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        var instances = mock(ProcessInstanceRepository.class);
        var system = mock(VUserSystemPermissionRepository.class);
        var permission = ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
        if (grant.equals("system")) when(system.hasPermission("actor", permission)).thenReturn(true);
        else when(instances.hasPermission("actor", grant.equals("instance") ? 12L : 99L, permission)).thenReturn(true);
        var scopedPermissions = new PermissionService(null, null, system, null, null, null, instances);
        var scopedController = new ProcessInstanceEventController(users, mock(ProcessInstanceEventService.class), logs, scopedPermissions);
        var pageable = PageRequest.of(0, 999);
        if (grant.equals("other-instance")) {
            assertThrows(ResponseException.class, () -> scopedController.getHistoryEvents(null, 12L, ProcessHistoryEventScope.ManualInterventions, null, pageable));
            verifyNoInteractions(logs);
        } else {
            scopedController.getHistoryEvents(null, 12L, ProcessHistoryEventScope.ManualInterventions, null, pageable);
            verify(logs).getHistoryEvents(12L, ProcessHistoryEventScope.ManualInterventions, null, pageable);
        }
    }

    @Test
    void historyUsesTheRequestedInstancePermissionBeforeLoading() throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        var pageable = PageRequest.of(0, 999);
        controller.getHistoryEvents(null, 12L, ProcessHistoryEventScope.ManualInterventions, null, pageable);
        var order = inOrder(permissions, logs);
        order.verify(permissions).requireProcessInstancePermission("actor", 12L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        order.verify(logs).getHistoryEvents(12L, ProcessHistoryEventScope.ManualInterventions, null, pageable);
    }

    @Test
    void historyRejectsAccessToAnotherInstanceBeforeLoading() throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        doThrow(ResponseException.forbidden()).when(permissions)
                .requireProcessInstancePermission("actor", 99L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> controller.getHistoryEvents(null, 99L, ProcessHistoryEventScope.ManualInterventions, null, PageRequest.of(0, 999)));
        verifyNoInteractions(logs);
    }

    @Test
    void historyRequiresAuthentication() throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.empty());
        assertThrows(ResponseException.class, () -> controller.getHistoryEvents(null, 12L, ProcessHistoryEventScope.ManualInterventions, null, PageRequest.of(0, 999)));
        verifyNoInteractions(permissions, logs);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void logFiltersAreForwardedAfterCheckingProcessInstanceAccess(boolean includeRestartHistory) throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        var pageable = PageRequest.of(0, 50);

        controller.getEventLog(null, 12L, 34L, includeRestartHistory, "Robin", true, false, "user-1", "identity-1", "Antrag", pageable);

        var order = inOrder(permissions, logs);
        order.verify(permissions).requireProcessInstancePermission("actor", 12L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        order.verify(logs).getEventLog(12L, 34L, includeRestartHistory, "Robin", true, false, "user-1", "identity-1", "Antrag", pageable);
    }

    @Test
    void concernedUserFilterCannotBypassProcessInstanceAccess() throws Exception {
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        doThrow(ResponseException.forbidden()).when(permissions)
                .requireProcessInstancePermission("actor", 99L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);

        assertThrows(ResponseException.class, () -> controller.getEventLog(
                null, 99L, 34L, true, null, false, false, "actor", null, null, PageRequest.of(0, 50)));

        verifyNoInteractions(logs);
    }
}
