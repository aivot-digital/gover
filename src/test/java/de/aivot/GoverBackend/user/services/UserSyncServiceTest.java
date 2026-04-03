package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSyncServiceTest {
    @Test
    void syncUsersShouldPersistRoleAssignmentEvenIfOnlySystemRoleChanged() throws ResponseException {
        var auditService = mock(AuditService.class);
        var scopedAuditService = mock(ScopedAuditService.class);
        var auditLogPayload = mock(AuditLogPayload.class);
        when(auditService.createScopedAuditService(UserSyncService.class, "Benutzer")).thenReturn(scopedAuditService);
        when(scopedAuditService.create()).thenReturn(auditLogPayload);
        when(auditLogPayload.withSystem()).thenReturn(auditLogPayload);
        when(auditLogPayload.setTriggerType(anyString())).thenReturn(auditLogPayload);
        when(auditLogPayload.setMessage(anyString())).thenReturn(auditLogPayload);
        when(auditLogPayload.setMetadata(anyMap())).thenReturn(auditLogPayload);

        var userRepository = mock(UserRepository.class);
        var keycloakApiService = mock(KeyCloakApiService.class);
        var importedUserSystemRoleService = mock(ImportedUserSystemRoleService.class);

        var existingUser = new UserEntity()
                .setId("user-1")
                .setEmail("user@example.org")
                .setFirstName("Max")
                .setLastName("Mustermann")
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false)
                .setSystemRoleId(null);

        var keycloakUser = new KeycloakUser()
                .setId("user-1")
                .setEmail("user@example.org")
                .setFirstName("Max")
                .setLastName("Mustermann")
                .setEnabled(true)
                .setEmailVerified(true);

        when(importedUserSystemRoleService.getDefaultSystemRoleId()).thenReturn(3);
        when(importedUserSystemRoleService.getSuperAdminRoleId()).thenReturn(1);
        when(importedUserSystemRoleService.hasSuperAdminUser(1)).thenReturn(true);
        when(importedUserSystemRoleService.resolveSystemRoleId("user@example.org", null, 3, 1, true))
                .thenReturn(new ImportedUserSystemRoleService.ImportedUserSystemRoleResolution(3, false));

        when(userRepository.findAll()).thenReturn(List.of(existingUser));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingUser));
        when(keycloakApiService.listUsers()).thenReturn(List.of(keycloakUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new UserSyncService(
                auditService,
                userRepository,
                keycloakApiService,
                importedUserSystemRoleService
        );

        service.syncUsers();

        var captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getSystemRoleId());
        assertEquals("user-1", captor.getValue().getId());
    }
}
