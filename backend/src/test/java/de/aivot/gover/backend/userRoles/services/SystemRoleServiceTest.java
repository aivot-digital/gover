package de.aivot.gover.backend.userRoles.services;

import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.gover.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.repositories.UserRepository;
import de.aivot.gover.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.gover.backend.userRoles.repositories.SystemRoleRepository;
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
    private static SystemRoleService createService(SystemRoleRepository repository,
                                                   SystemConfigService systemConfigService,
                                                   UserRepository userRepository) {
        return new SystemRoleService(repository, systemConfigService, userRepository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
    }

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

        var service = createService(repository, systemConfigService, userRepository);
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
        when(userRepository.findAllBySystemRoleIdOrderByFullNameAsc(4)).thenReturn(List.of());

        var service = createService(repository, systemConfigService, userRepository);
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
        assertEquals(List.of(), result.migratedUsers());
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
        when(userRepository.findAllBySystemRoleIdOrderByFullNameAsc(3)).thenReturn(List.of(
                new UserEntity()
                        .setId("user-1")
                        .setFullName("Erika Musterfrau")
                        .setEmail("erika.musterfrau@example.org"),
                new UserEntity()
                        .setId("user-2")
                        .setFullName("Max Mustermann")
                        .setEmail("max.mustermann@example.org")
        ));
        when(userRepository.reassignSystemRoleId(3, 7)).thenReturn(5);

        var replacementRole = new SystemRoleEntity()
                .setId(7)
                .setName("Sachbearbeitung")
                .setPermissions(List.of());
        when(repository.findById(7)).thenReturn(Optional.of(replacementRole));

        var service = createService(repository, systemConfigService, userRepository);
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
        assertEquals(2, result.migratedUsers().size());
        assertEquals("user-1", result.migratedUsers().get(0).id());
        assertEquals("Erika Musterfrau", result.migratedUsers().get(0).fullName());
        assertEquals("erika.musterfrau@example.org", result.migratedUsers().get(0).email());
    }

    @Test
    void createShouldRejectPermissionsThatAreUnknown() {
        var repository = mock(SystemRoleRepository.class);
        var service = createService(repository, mock(SystemConfigService.class), mock(UserRepository.class));
        var entity = new SystemRoleEntity()
                .setPermissions(List.of("removed.permission"));

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(entity);
    }

    @Test
    void updateShouldAllowExistingPermissionsThatAreNoLongerKnown() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var service = createService(repository, mock(SystemConfigService.class), mock(UserRepository.class));
        var existingEntity = new SystemRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        "removed.permission"
                ));
        var updatedEntity = new SystemRoleEntity()
                .setName("Updated")
                .setDescription("Updated description")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        "removed.permission"
                ));

        service.performUpdate(1, updatedEntity, existingEntity);

        verify(repository).save(existingEntity);
    }

    @Test
    void updateShouldRejectNewPermissionsThatAreUnknown() {
        var repository = mock(SystemRoleRepository.class);
        var service = createService(repository, mock(SystemConfigService.class), mock(UserRepository.class));
        var existingEntity = new SystemRoleEntity()
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_READ));
        var updatedEntity = new SystemRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        "removed.permission"
                ));

        var exception = assertThrows(ResponseException.class, () -> service.performUpdate(1, updatedEntity, existingEntity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(existingEntity);
    }

    @Test
    void updateShouldAllowRemovingPermissionsThatAreNoLongerKnown() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var service = createService(repository, mock(SystemConfigService.class), mock(UserRepository.class));
        var existingEntity = new SystemRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        "removed.permission"
                ));
        var updatedEntity = new SystemRoleEntity()
                .setName("Updated")
                .setDescription("Updated description")
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_READ));

        service.performUpdate(1, updatedEntity, existingEntity);

        verify(repository).save(existingEntity);
    }
}
