package de.aivot.prosuna.backend.teams.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.services.TeamService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
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

class TeamControllerTest {
    private final UserService userService = mock(UserService.class);
    private final TeamService teamService = mock(TeamService.class);
    private final TeamController controller = new TeamController(
            mock(AuditService.class, RETURNS_DEEP_STUBS),
            userService,
            teamService,
            mock(PermissionService.class)
    );

    @BeforeEach
    void setup() throws ResponseException {
        when(userService.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("staff")));
    }

    @Test
    void updateKeepsTheConflictOfAnOutdatedLockVersion() throws ResponseException {
        var conflict = ResponseException.conflict(LockVersioned.CONFLICT_MESSAGE);
        when(teamService.update(eq(17), any())).thenThrow(conflict);

        assertSame(conflict, assertThrows(ResponseException.class, () -> controller.update(null, 17, new TeamEntity())));
    }

    @Test
    void updatePassesAConcurrentCommitToTheConflictHandler() throws ResponseException {
        var conflict = new ObjectOptimisticLockingFailureException(TeamEntity.class, 17);
        when(teamService.update(eq(17), any())).thenThrow(conflict);

        assertSame(conflict, assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> controller.update(null, 17, new TeamEntity())));
    }

    @Test
    void updateReportsOtherFailuresAsBadRequest() throws ResponseException {
        when(teamService.update(eq(17), any())).thenThrow(ResponseException.notFound());

        var exception = assertThrows(ResponseException.class, () -> controller.update(null, 17, new TeamEntity()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
