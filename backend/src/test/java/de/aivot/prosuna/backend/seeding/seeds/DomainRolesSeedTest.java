package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.UserRoleRepository;
import de.aivot.prosuna.backend.userRoles.services.UserRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainRolesSeedTest {
    @Test
    void shouldSeedOnlyWithoutExistingDomainRoles() {
        var repository = mock(UserRoleRepository.class);
        var seed = new DomainRolesSeed(mock(UserRoleService.class), repository);

        when(repository.count()).thenReturn(0L);
        assertTrue(seed.shouldSeed());

        when(repository.count()).thenReturn(1L);
        assertFalse(seed.shouldSeed());
    }

    @Test
    void seedShouldCreateDefaultDomainRoles() throws ResponseException {
        var repository = mock(UserRoleRepository.class);
        when(repository.save(any(UserRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // The real service validates that every seeded permission can be assigned to a domain role.
        var userRoleService = new UserRoleService(repository, SystemRolesSeedTest.allPermissionProviders());
        var seed = new DomainRolesSeed(userRoleService, repository);

        seed.seed();

        var roles = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(repository, times(6)).save(roles.capture());
        assertEquals(
                List.of("Domänenadministration", "Sachbearbeitung", "Modellierung", "Prüfung", "Prozessmanagement", "Betrachtung"),
                roles.getAllValues().stream().map(UserRoleEntity::getName).toList()
        );
    }
}
