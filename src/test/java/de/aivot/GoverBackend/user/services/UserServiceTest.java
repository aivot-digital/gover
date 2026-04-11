package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Test
    void importUserFromKeycloakShouldAssignResolvedSystemRole() throws ResponseException {
        var keycloakApiService = mock(KeyCloakApiService.class);
        var importedUserSystemRoleService = mock(ImportedUserSystemRoleService.class);
        var processInstanceTaskRepository = mock(ProcessInstanceTaskRepository.class);
        var userRepository = mock(UserRepository.class);

        when(keycloakApiService.retrieveUser("user-1"))
                .thenReturn(Optional.of(new KeycloakUser()
                        .setId("user-1")
                        .setEmail("user@example.org")
                        .setFirstName("Max")
                        .setLastName("Mustermann")
                        .setEnabled(true)
                        .setEmailVerified(true)));
        when(importedUserSystemRoleService.resolveSystemRoleId("user@example.org", null))
                .thenReturn(new ImportedUserSystemRoleService.ImportedUserSystemRoleResolution(3, false));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new UserService(
                keycloakApiService,
                importedUserSystemRoleService,
                processInstanceTaskRepository,
                userRepository
        );

        var importedUser = service.importUserFromKeycloak("user-1");

        assertTrue(importedUser.isPresent());
        assertEquals(3, importedUser.get().getSystemRoleId());
        assertEquals("user@example.org", importedUser.get().getEmail());
    }
}
