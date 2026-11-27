package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.userRoles.configs.MostPrivilegedSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import de.aivot.prosuna.backend.userRoles.services.SystemRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    // Discover the providers instead of listing them, so the default roles cannot silently miss a new provider.
    static List<PermissionProvider> allPermissionProviders() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(PermissionProvider.class));
        return scanner.findCandidateComponents("de.aivot.prosuna.backend").stream()
                .map(BeanDefinition::getBeanClassName)
                .sorted()
                .map(SystemRolesSeedTest::createPermissionProvider)
                .toList();
    }

    private static PermissionProvider createPermissionProvider(String className) {
        try {
            return (PermissionProvider) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Permission provider " + className + " needs a no-argument constructor.", e);
        }
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

        // The super administrator role must grant every permission that any provider defines.
        var allPermissions = allPermissionProviders().stream()
                .flatMap(provider -> Arrays.stream(provider.getPermissions()))
                .map(PermissionEntry::permission)
                .collect(Collectors.toSet());
        assertEquals(allPermissions, Set.copyOf(roles.getAllValues().getFirst().getPermissions()));
        // The service does not remove repeated keys, so they would be stored as duplicate grants.
        for (var role : roles.getAllValues()) {
            assertEquals(Set.copyOf(role.getPermissions()).size(), role.getPermissions().size(),
                    role.getName() + " must not grant a permission more than once.");
        }
        assertTrue(roles.getAllValues().getLast().getPermissions().contains(CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ));

        var mostPrivilegedRoleConfig = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(systemConfigService).save(eq(MostPrivilegedSystemRoleSystemConfigDefinition.KEY), mostPrivilegedRoleConfig.capture());
        assertEquals("7", mostPrivilegedRoleConfig.getValue().getValue());

        var defaultRoleConfig = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(systemConfigService).save(eq(DefaultUserSystemRoleSystemConfigDefinition.KEY), defaultRoleConfig.capture());
        assertEquals("9", defaultRoleConfig.getValue().getValue());
    }
}
