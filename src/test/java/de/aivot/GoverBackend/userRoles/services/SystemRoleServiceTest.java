package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemRoleServiceTest {
    @Test
    void performDeleteShouldRejectConfiguredDefaultRole() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var systemConfigService = mock(SystemConfigService.class);

        when(systemConfigService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("3")
                        .setPublicConfig(false));

        var service = new SystemRoleService(repository, systemConfigService);
        var entity = new SystemRoleEntity()
                .setId(3)
                .setName("Standard-Mitarbeiter:in")
                .setPermissions(List.of());

        var exception = assertThrows(ResponseException.class, () -> service.performDelete(entity));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(
                "Die konfigurierte Standard-Systemrolle für automatische Benutzerimporte kann nicht gelöscht werden. Wählen Sie zuerst eine andere Standard-Systemrolle aus.",
                exception.getTitle()
        );
        verify(repository, never()).delete(entity);
    }

    @Test
    void performDeleteShouldAllowDeletingNonDefaultRole() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var systemConfigService = mock(SystemConfigService.class);

        when(systemConfigService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("3")
                        .setPublicConfig(false));

        var service = new SystemRoleService(repository, systemConfigService);
        var entity = new SystemRoleEntity()
                .setId(4)
                .setName("Systemadministrator:in")
                .setPermissions(List.of());

        assertDoesNotThrow(() -> service.performDelete(entity));

        verify(repository).delete(entity);
    }
}
