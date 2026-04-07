package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemRoleServiceTest {
    @Test
    void performDeleteShouldRejectDeletingRoleWithAssignedUsersWithoutReplacement() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var systemConfigService = mock(SystemConfigService.class);
        var userRepository = mock(UserRepository.class);

        when(systemConfigService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("3")
                        .setPublicConfig(false));
        when(userRepository.existsBySystemRoleId(4)).thenReturn(true);

        var service = new SystemRoleService(repository, systemConfigService, userRepository);
        var entity = new SystemRoleEntity()
                .setId(4)
                .setName("Sachbearbeitung")
                .setPermissions(List.of());

        var exception = assertThrows(ResponseException.class, () -> service.performDelete(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Bitte wählen Sie eine Ersatz-Systemrolle aus, damit vorhandene Nutzer:innen und Systemeinstellungen migriert werden können.",
                exception.getTitle()
        );
        verify(repository, never()).delete(entity);
    }

    @Test
    void deleteAndMigrateUsersShouldDeleteUnusedNonDefaultRoleWithoutReplacement() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var systemConfigService = mock(SystemConfigService.class);
        var userRepository = mock(UserRepository.class);

        when(systemConfigService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("3")
                        .setPublicConfig(false));
        when(userRepository.existsBySystemRoleId(4)).thenReturn(false);

        var service = new SystemRoleService(repository, systemConfigService, userRepository);
        var entity = new SystemRoleEntity()
                .setId(4)
                .setName("Systemadministrator:in")
                .setPermissions(List.of());

        var result = service.deleteAndMigrateUsers(entity, null);

        verify(repository).delete(entity);
        verify(userRepository, never()).reassignSystemRoleId(any(), any());
        verify(systemConfigService, never()).save(eq(DefaultUserSystemRoleSystemConfigDefinition.KEY), any());
        assertEquals(0, result.migratedUsersCount());
        assertEquals(false, result.defaultSystemRoleForAutomaticImportsUpdated());
        assertNull(result.replacementRole());
        assertNull(result.newDefaultSystemRoleId());
    }

    @Test
    void deleteAndMigrateUsersShouldUpdateUsersAndDefaultRoleWhenDeletingConfiguredDefaultRole() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var systemConfigService = mock(SystemConfigService.class);
        var userRepository = mock(UserRepository.class);

        var defaultSystemRoleConfig = new SystemConfigEntity()
                .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                .setValue("3")
                .setPublicConfig(false);

        when(systemConfigService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(defaultSystemRoleConfig);
        when(userRepository.existsBySystemRoleId(3)).thenReturn(true);
        when(userRepository.reassignSystemRoleId(3, 7)).thenReturn(5);

        var replacementRole = new SystemRoleEntity()
                .setId(7)
                .setName("Sachbearbeitung")
                .setPermissions(List.of());
        when(repository.findById(7)).thenReturn(Optional.of(replacementRole));

        var service = new SystemRoleService(repository, systemConfigService, userRepository);
        var entity = new SystemRoleEntity()
                .setId(3)
                .setName("Mitarbeiter:in")
                .setPermissions(List.of());

        var result = service.deleteAndMigrateUsers(entity, 7);

        verify(userRepository).reassignSystemRoleId(3, 7);
        verify(systemConfigService).save(DefaultUserSystemRoleSystemConfigDefinition.KEY, defaultSystemRoleConfig);
        verify(repository).delete(entity);
        assertEquals(5, result.migratedUsersCount());
        assertEquals(true, result.defaultSystemRoleForAutomaticImportsUpdated());
        assertEquals(7, result.replacementRole().getId());
        assertEquals(7, result.newDefaultSystemRoleId());
    }
}
