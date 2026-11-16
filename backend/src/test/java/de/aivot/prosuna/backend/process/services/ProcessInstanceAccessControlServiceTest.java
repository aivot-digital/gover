package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceAccessRuleDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAccessControlRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProcessInstanceAccessControlServiceTest {
    private static final long INSTANCE_ID = 17L;

    private final ProcessInstanceAccessControlRepository repository = mock(ProcessInstanceAccessControlRepository.class);
    private final ProcessInstanceAccessGuard guard = mock(ProcessInstanceAccessGuard.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final UserEntity actor = new UserEntity();
    private ProcessInstanceAccessControlEntity stored;
    private ProcessInstanceAccessControlService service;

    @BeforeEach
    void setUp() throws ResponseException {
        var auditService = mock(AuditService.class);
        when(auditService.createScopedAuditService(any(), anyString())).thenReturn(mock(ScopedAuditService.class, RETURNS_DEEP_STUBS));
        service = new ProcessInstanceAccessControlService(repository, guard, permissions, auditService);
        actor.setId("user-1");
        stored = new ProcessInstanceAccessControlEntity()
                .setId(3)
                .setTargetProcessInstanceId(INSTANCE_ID)
                .setSourceTeamId(8)
                .setPermissions(List.of(PROCESS_INSTANCE_READ))
                .setLockVersion(2L);
        when(repository.findAllByTargetProcessInstanceIdOrderById(INSTANCE_ID)).thenReturn(List.of(stored));
        when(guard.lockAndSnapshot(INSTANCE_ID)).thenReturn(Map.of());
    }

    @Test
    void replacesRulesBasedOnTheStoredState() throws ResponseException {
        service.replace(actor, INSTANCE_ID, List.of(rule(PROCESS_INSTANCE_UPDATE)), "3:2");

        assertEquals(List.of(PROCESS_INSTANCE_UPDATE), stored.getPermissions());
        verify(repository).save(stored);
    }

    @Test
    void rejectsAChangedRuleWithoutChangingIt() {
        assertConflict("3:1");
    }

    @Test
    void rejectsARuleAddedMeanwhileWithoutDeletingIt() {
        // The client loaded no rules, so the stored one was added by someone else.
        assertConflict("");
    }

    @Test
    void rejectsARuleRemovedMeanwhile() {
        assertConflict("3:2,4:0");
    }

    @Test
    void rejectsAMalformedState() {
        var exception = assertThrows(ResponseException.class,
                () -> service.replace(actor, INSTANCE_ID, List.of(rule(PROCESS_INSTANCE_UPDATE)), "3"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void lastSaveWinsWithoutAStatedState() throws ResponseException {
        stored.setLockVersion(5L);

        service.replace(actor, INSTANCE_ID, List.of(rule(PROCESS_INSTANCE_UPDATE)), null);

        assertEquals(List.of(PROCESS_INSTANCE_UPDATE), stored.getPermissions());
    }

    private void assertConflict(String basedOn) {
        var exception = assertThrows(ResponseException.class,
                () -> service.replace(actor, INSTANCE_ID, List.of(rule(PROCESS_INSTANCE_UPDATE)), basedOn));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(LockVersioned.CONFLICT_DETAILS, exception.getDetails());
        assertEquals(List.of(PROCESS_INSTANCE_READ), stored.getPermissions());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteAll(any());
    }

    private static ProcessInstanceAccessRuleDTO rule(String permission) {
        return new ProcessInstanceAccessRuleDTO(null, 8, List.of(permission));
    }
}
