package de.aivot.prosuna.backend.lib.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateEntityServiceTest {
    private UpdateEntityService<TeamEntity, Integer> service;
    private TeamEntity existing;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws ResponseException {
        service = mock(UpdateEntityService.class, CALLS_REAL_METHODS);
        existing = new TeamEntity().setId(17).setName("Bauamt").setLockVersion(3L);
        doReturn(Optional.of(existing)).when(service).retrieve(17);
        doReturn(existing).when(service).performUpdate(eq(17), any(), any());
    }

    @Test
    void outdatedLockVersionIsRejectedBeforeTheUpdate() throws ResponseException {
        var request = new TeamEntity().setName("Ordnungsamt").setLockVersion(2L);

        var exception = assertThrows(ResponseException.class, () -> service.update(17, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(LockVersioned.CONFLICT_MESSAGE, exception.getTitle());
        assertEquals(Map.of("reason", "lock_version_conflict"), exception.getDetails());
        verify(service, never()).performUpdate(any(), any(), any());
    }

    @Test
    void currentLockVersionIsUpdated() throws ResponseException {
        var request = new TeamEntity().setName("Ordnungsamt").setLockVersion(3L);

        assertSame(existing, service.update(17, request));

        verify(service).performUpdate(17, request, existing);
    }

    @Test
    void requestWithoutLockVersionIsUpdated() throws ResponseException {
        var request = new TeamEntity().setName("Ordnungsamt");

        assertSame(existing, service.update(17, request));

        verify(service).performUpdate(17, request, existing);
    }

    @Test
    void entitiesWithoutLockVersionAreNotChecked() throws ResponseException {
        LockVersioned.requireCurrent(new Object(), existing);
        LockVersioned.requireCurrent(new TeamEntity().setLockVersion(1L), new Object());
    }
}
