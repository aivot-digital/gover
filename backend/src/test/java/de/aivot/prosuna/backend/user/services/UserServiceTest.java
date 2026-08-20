package de.aivot.prosuna.backend.user.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.models.KeycloakUser;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importUserFromKeycloakShouldAssignResolvedSystemRole() throws ResponseException {
        var keycloakApiService = mock(KeyCloakApiService.class);
        var importedUserSystemRoleService = mock(ImportedUserSystemRoleService.class);
        var permissionService = mock(PermissionService.class);
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
                permissionService,
                processInstanceTaskRepository,
                userRepository
        );

        var importedUser = service.importUserFromKeycloak("user-1");

        assertTrue(importedUser.isPresent());
        assertEquals(3, importedUser.get().getSystemRoleId());
        assertEquals("user@example.org", importedUser.get().getEmail());
    }

    @Test
    void performUpdateShouldKeepAdministrativeFieldsWithoutUserUpdatePermission() throws ResponseException {
        var fixture = createUpdateFixture(false);

        var result = fixture.service().performUpdate("user-2", fixture.update(), fixture.existing());

        assertTrue(result.getEnabled());
        assertEquals(1, result.getSystemRoleId());
        verify(fixture.permissionService())
                .hasSystemPermission(fixture.jwt(), UserPermissionProvider.USER_UPDATE);
    }

    @Test
    void performUpdateShouldChangeAdministrativeFieldsWithUserUpdatePermission() throws ResponseException {
        var fixture = createUpdateFixture(true);

        var result = fixture.service().performUpdate("user-2", fixture.update(), fixture.existing());

        assertFalse(result.getEnabled());
        assertEquals(2, result.getSystemRoleId());
        verify(fixture.permissionService())
                .hasSystemPermission(fixture.jwt(), UserPermissionProvider.USER_UPDATE);
    }

    private UpdateFixture createUpdateFixture(boolean hasUserUpdatePermission) throws ResponseException {
        var keycloakApiService = mock(KeyCloakApiService.class);
        var importedUserSystemRoleService = mock(ImportedUserSystemRoleService.class);
        var permissionService = mock(PermissionService.class);
        var processInstanceTaskRepository = mock(ProcessInstanceTaskRepository.class);
        var userRepository = mock(UserRepository.class);
        var jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var existing = new UserEntity()
                .setId("user-2")
                .setEmail("old@example.org")
                .setFirstName("Old")
                .setLastName("Name")
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false)
                .setSystemRoleId(1);
        var update = new UserEntity()
                .setId("user-2")
                .setEmail("new@example.org")
                .setFirstName("New")
                .setLastName("Name")
                .setEnabled(false)
                .setVerified(true)
                .setDeletedInIdp(false)
                .setSystemRoleId(2);

        when(permissionService.hasSystemPermission(jwt, UserPermissionProvider.USER_UPDATE))
                .thenReturn(hasUserUpdatePermission);
        when(keycloakApiService.updateUser(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return new UpdateFixture(
                new UserService(
                        keycloakApiService,
                        importedUserSystemRoleService,
                        permissionService,
                        processInstanceTaskRepository,
                        userRepository
                ),
                permissionService,
                jwt,
                existing,
                update
        );
    }

    private record UpdateFixture(
            UserService service,
            PermissionService permissionService,
            Jwt jwt,
            UserEntity existing,
            UserEntity update
    ) {
    }
}
