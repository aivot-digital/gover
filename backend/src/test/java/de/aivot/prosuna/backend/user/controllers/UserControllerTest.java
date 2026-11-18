package de.aivot.prosuna.backend.user.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.KeyCloakApiService;
import de.aivot.prosuna.backend.user.services.UserProvisioningService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {
    private final UserService userService = mock(UserService.class);
    private final UserController controller = new UserController(
            mock(AuditService.class, RETURNS_DEEP_STUBS),
            userService,
            mock(UserProvisioningService.class),
            mock(KeyCloakApiService.class),
            mock(PermissionService.class)
    );

    @BeforeEach
    void setup() throws ResponseException {
        when(userService.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("staff")));
    }

    @Test
    void updateKeepsTheConflictOfAnOutdatedLockVersion() throws ResponseException {
        var conflict = ResponseException.conflict(LockVersioned.CONFLICT_MESSAGE);
        when(userService.update(eq("staff"), any())).thenThrow(conflict);

        assertSame(conflict, assertThrows(ResponseException.class, () -> controller.update(null, "staff", new UserEntity())));
    }

    @Test
    void updatePassesAConcurrentCommitToTheConflictHandler() throws ResponseException {
        var conflict = new ObjectOptimisticLockingFailureException(UserEntity.class, "staff");
        when(userService.update(eq("staff"), any())).thenThrow(conflict);

        assertSame(conflict, assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> controller.update(null, "staff", new UserEntity())));
    }

    @Test
    void updateReportsOtherFailuresAsBadRequest() throws ResponseException {
        when(userService.update(eq("staff"), any())).thenThrow(ResponseException.notFound());

        var exception = assertThrows(ResponseException.class, () -> controller.update(null, "staff", new UserEntity()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
