package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
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
