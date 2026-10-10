package de.aivot.prosuna.backend.user.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeputyControllerTest {
    private static final String EXEC_USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final String ADMIN_USER_ID = "admin-1";
    private static final Integer RELATION_ID = 5;

    private final Jwt jwt = mock(Jwt.class);
    private final UserService userService = mock(UserService.class);
    private final UserDeputyService userDeputyService = mock(UserDeputyService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final UserEntity user = mock(UserEntity.class);

    private UserDeputyController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        when(user.getId()).thenReturn(EXEC_USER_ID);
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(user));
        controller = new UserDeputyController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                userService,
                userDeputyService,
                permissionService
        );
    }

    @Test
    void createRejectsAssigningSelfAsDeputyOfAnotherUserWithoutPermission() throws ResponseException {
        var relation = createRelation(ADMIN_USER_ID, EXEC_USER_ID);

        assertForbidden(() -> controller.create(jwt, relation));
        verify(userDeputyService, never()).create(any());
    }

    @Test
    void createRejectsDelegatingOwnPermissionsWithoutPermission() throws ResponseException {
        var relation = createRelation(EXEC_USER_ID, OTHER_USER_ID);

        assertForbidden(() -> controller.create(jwt, relation));
        verify(userDeputyService, never()).create(any());
    }

    @Test
    void createRequiresCreatePermissionInsteadOfReadPermission() {
        grantSystemPermission(UserPermissionProvider.DEPUTY_READ);

        assertForbidden(() -> controller.checkCreatePermissions(user, createRelation(ADMIN_USER_ID, EXEC_USER_ID)));
    }

    @Test
    void createWithSystemPermissionAllowsAnyRelation() {
        grantSystemPermission(UserPermissionProvider.DEPUTY_CREATE);

        assertDoesNotThrow(() -> controller.checkCreatePermissions(user, createRelation(ADMIN_USER_ID, OTHER_USER_ID)));
    }

    @Test
    void updateRejectsDeputyExtendingOwnRelationWithoutPermission() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, EXEC_USER_ID));
        var patch = createRelation(ADMIN_USER_ID, EXEC_USER_ID).setUntilDate(null);

        assertForbidden(() -> controller.update(jwt, RELATION_ID, patch));
        verify(userDeputyService, never()).update(anyInt(), any());
    }

    @Test
    void updateRejectsOriginalUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(EXEC_USER_ID, OTHER_USER_ID));

        assertForbidden(() -> controller.checkUpdatePermission(user, RELATION_ID));
    }

    @Test
    void updateWithSystemPermissionAllowsAnyRelation() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, OTHER_USER_ID));
        grantSystemPermission(UserPermissionProvider.DEPUTY_UPDATE);

        assertDoesNotThrow(() -> controller.checkUpdatePermission(user, RELATION_ID));
    }

    @Test
    void deleteAllowsOriginalUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(EXEC_USER_ID, OTHER_USER_ID));

        assertDoesNotThrow(() -> controller.checkDeletePermission(user, RELATION_ID));
    }

    @Test
    void deleteAllowsDeputyUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(OTHER_USER_ID, EXEC_USER_ID));

        assertDoesNotThrow(() -> controller.checkDeletePermission(user, RELATION_ID));
    }

    @Test
    void deleteRejectsUnrelatedUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, OTHER_USER_ID));

        assertForbidden(() -> controller.checkDeletePermission(user, RELATION_ID));
    }

    @Test
    void deleteWithSystemPermissionAllowsAnyRelation() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, OTHER_USER_ID));
        grantSystemPermission(UserPermissionProvider.DEPUTY_DELETE);

        assertDoesNotThrow(() -> controller.checkDeletePermission(user, RELATION_ID));
    }

    @Test
    void retrieveAllowsRelatedUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, EXEC_USER_ID));

        assertDoesNotThrow(() -> controller.checkRetrievePermissions(user, RELATION_ID));
    }

    @Test
    void retrieveRejectsUnrelatedUserWithoutPermission() throws ResponseException {
        storeRelation(createRelation(ADMIN_USER_ID, OTHER_USER_ID));

        assertForbidden(() -> controller.checkRetrievePermissions(user, RELATION_ID));
    }

    private void grantSystemPermission(String permission) {
        when(permissionService.hasSystemPermission(EXEC_USER_ID, permission)).thenReturn(true);
    }

    private void storeRelation(UserDeputyEntity relation) throws ResponseException {
        when(userDeputyService.retrieve(RELATION_ID)).thenReturn(Optional.of(relation));
    }

    private static UserDeputyEntity createRelation(String originalUserId, String deputyUserId) {
        return new UserDeputyEntity()
                .setId(RELATION_ID)
                .setOriginalUserId(originalUserId)
                .setDeputyUserId(deputyUserId)
                .setFromDate(LocalDate.of(2026, 4, 1));
    }

    private static void assertForbidden(Executable executable) {
        var exception = assertThrows(ResponseException.class, executable);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }
}
