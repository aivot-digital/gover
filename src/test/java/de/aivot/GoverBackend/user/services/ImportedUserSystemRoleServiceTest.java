package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportedUserSystemRoleServiceTest {
    @Test
    void getDefaultSystemRoleIdShouldReturnConfiguredRole() throws ResponseException {
        var goverConfig = new GoverConfig();
        var configService = mock(SystemConfigService.class);
        var systemRoleRepository = mock(SystemRoleRepository.class);
        var userRepository = mock(UserRepository.class);

        when(configService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("3")
                        .setPublicConfig(false));
        when(systemRoleRepository.existsById(3)).thenReturn(true);

        var service = new ImportedUserSystemRoleService(goverConfig, configService, systemRoleRepository, userRepository);

        assertEquals(3, service.getDefaultSystemRoleId());
    }

    @Test
    void getDefaultSystemRoleIdShouldRejectUnknownConfiguredRole() throws ResponseException {
        var goverConfig = new GoverConfig();
        var configService = mock(SystemConfigService.class);
        var systemRoleRepository = mock(SystemRoleRepository.class);
        var userRepository = mock(UserRepository.class);

        when(configService.retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY))
                .thenReturn(new SystemConfigEntity()
                        .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                        .setValue("999")
                        .setPublicConfig(false));
        when(systemRoleRepository.existsById(999)).thenReturn(false);

        var service = new ImportedUserSystemRoleService(goverConfig, configService, systemRoleRepository, userRepository);

        var exception = assertThrows(ResponseException.class, service::getDefaultSystemRoleId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Die konfigurierte Standard-Systemrolle für automatische Benutzerimporte existiert nicht.", exception.getTitle());
    }

    @Test
    void resolveSystemRoleIdShouldPromoteBootstrapAdminIfNoSuperAdminExists() {
        var goverConfig = new GoverConfig();
        goverConfig.setBootstrapAdminMail(List.of("admin@example.org"));

        var service = new ImportedUserSystemRoleService(
                goverConfig,
                mock(SystemConfigService.class),
                mock(SystemRoleRepository.class),
                mock(UserRepository.class)
        );

        var resolution = service.resolveSystemRoleId("admin@example.org", null, 3, 1, false);

        assertEquals(1, resolution.systemRoleId());
        assertEquals(true, resolution.promotedToSuperAdmin());
    }

    @Test
    void resolveSystemRoleIdShouldKeepExistingRoleWhenPresent() {
        var service = new ImportedUserSystemRoleService(
                new GoverConfig(),
                mock(SystemConfigService.class),
                mock(SystemRoleRepository.class),
                mock(UserRepository.class)
        );

        var resolution = service.resolveSystemRoleId("staff@example.org", 7, 3, 1, true);

        assertEquals(7, resolution.systemRoleId());
        assertFalse(resolution.promotedToSuperAdmin());
    }

    @Test
    void resolveSystemRoleIdShouldAssignDefaultRoleWhenCurrentRoleMissing() {
        var service = new ImportedUserSystemRoleService(
                new GoverConfig(),
                mock(SystemConfigService.class),
                mock(SystemRoleRepository.class),
                mock(UserRepository.class)
        );

        var resolution = service.resolveSystemRoleId("staff@example.org", null, 3, 1, true);

        assertEquals(3, resolution.systemRoleId());
        assertFalse(resolution.promotedToSuperAdmin());
    }
}
