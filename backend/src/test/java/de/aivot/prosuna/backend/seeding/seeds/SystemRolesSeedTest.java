package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.asset.permissions.AssetPermissionProvider;
import de.aivot.prosuna.backend.audit.permissions.AuditPermissionProvider;
import de.aivot.prosuna.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.identity.permissions.IdentityProviderPermissionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.DomainResourcePermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.prosuna.backend.plugin.permissions.PluginPermissionProvider;
import de.aivot.prosuna.backend.preset.permissions.PresetPermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.secrets.permissions.SecretPermissionProvider;
import de.aivot.prosuna.backend.storage.permissions.StoragePermissionProvider;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.theme.permissions.ThemePermissionProvider;
import de.aivot.prosuna.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.userRoles.configs.MostPrivilegedSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.prosuna.backend.userRoles.permissions.DomainRolePermissionProvider;
import de.aivot.prosuna.backend.userRoles.permissions.SystemRolePermissionProvider;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import de.aivot.prosuna.backend.userRoles.services.SystemRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemRolesSeedTest {
    static List<PermissionProvider> allPermissionProviders() {
        return List.of(
                new AssetPermissionProvider(),
                new AuditPermissionProvider(),
                new CodeListPermissionProvider(),
                new CommunicationProviderPermissionProvider(),
                new ConfigPermissionProvider(),
                new DataObjectPermissionProvider(),
                new DepartmentPermissionProvider(),
                new DomainResourcePermissionProvider(),
                new DomainRolePermissionProvider(),
                new IdentityProviderPermissionProvider(),
                new PaymentProviderPermissionProvider(),
                new PermissionSetPermissionProvider(),
                new PluginPermissionProvider(),
                new PresetPermissionProvider(),
                new ProcessInstancePermissionProvider(),
                new ProcessPermissionProvider(),
                new SecretPermissionProvider(),
                new StoragePermissionProvider(),
                new SystemRolePermissionProvider(),
                new TeamPermissionProvider(),
                new ThemePermissionProvider(),
                new UserPermissionProvider()
        );
    }

    @Test
    void shouldSeedOnlyWithoutExistingSystemRoles() {
        var repository = mock(SystemRoleRepository.class);
        var seed = new SystemRolesSeed(mock(SystemRoleService.class), repository, mock(SystemConfigService.class));

        when(repository.count()).thenReturn(0L);
        assertTrue(seed.shouldSeed());

        when(repository.count()).thenReturn(1L);
        assertFalse(seed.shouldSeed());
    }

    @Test
    void seedShouldCreateDefaultSystemRolesAndReferenceThemInSystemConfigs() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        // IDs differ from the configs' default IDs, so the test shows that the configs reference the created roles.
        var nextId = new AtomicInteger(7);
        when(repository.save(any(SystemRoleEntity.class))).thenAnswer(invocation -> {
            SystemRoleEntity entity = invocation.getArgument(0);
            return entity.setId(nextId.getAndIncrement());
        });
        var systemConfigService = mock(SystemConfigService.class);
        // The real service validates that every seeded permission is known to a permission provider.
        var systemRoleService = new SystemRoleService(repository, systemConfigService, mock(UserRepository.class), allPermissionProviders());
        var seed = new SystemRolesSeed(systemRoleService, repository, systemConfigService);

        seed.seed();

        var roles = ArgumentCaptor.forClass(SystemRoleEntity.class);
        verify(repository, times(3)).save(roles.capture());
        assertEquals(
                List.of("Superadministrator:in", "Systemadministrator:in", "Mitarbeiter:in"),
                roles.getAllValues().stream().map(SystemRoleEntity::getName).toList()
        );

        var superAdministratorPermissions = roles.getAllValues().getFirst().getPermissions();
        assertTrue(superAdministratorPermissions.contains(AuditPermissionProvider.AUDIT_LOG_READ));
        assertTrue(superAdministratorPermissions.contains(CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_DELETE));
        assertTrue(roles.getAllValues().getLast().getPermissions().contains(CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ));

        var mostPrivilegedRoleConfig = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(systemConfigService).save(eq(MostPrivilegedSystemRoleSystemConfigDefinition.KEY), mostPrivilegedRoleConfig.capture());
        assertEquals("7", mostPrivilegedRoleConfig.getValue().getValue());

        var defaultRoleConfig = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(systemConfigService).save(eq(DefaultUserSystemRoleSystemConfigDefinition.KEY), defaultRoleConfig.capture());
        assertEquals("9", defaultRoleConfig.getValue().getValue());
    }
}
