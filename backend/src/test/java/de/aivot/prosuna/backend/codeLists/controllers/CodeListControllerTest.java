package de.aivot.prosuna.backend.codeLists.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.prosuna.backend.codeLists.services.CodeListService;
import de.aivot.prosuna.backend.codeLists.services.CodeListWorker;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CodeListControllerTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final CodeListElementOptionsService codeListElementOptionsService = mock(CodeListElementOptionsService.class);
    private final CodeListService service = mock(CodeListService.class);
    private final UserService userService = mock(UserService.class);
    private final CodeListWorker worker = mock(CodeListWorker.class);
    private final Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "user-1")
    );

    private CodeListController controller;

    @BeforeEach
    void setUp() {
        controller = new CodeListController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                service,
                userService,
                worker,
                permissionService,
                codeListElementOptionsService
        );
    }

    @Test
    void listOptionsRequiresTheReadPermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.listOptions(jwt, "items", "id", "name", "group_id")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(codeListElementOptionsService);
    }

    @Test
    void listOptionsReturnsTheOptionsOfTheRequestedColumns() throws ResponseException {
        var options = List.of(SelectInputElementOption.of("a", "Item A", "1"));
        when(codeListElementOptionsService.listOptions("items", "id", "name", "group_id")).thenReturn(options);

        var result = controller.listOptions(jwt, "items", "id", "name", "group_id");

        assertEquals(options, result);
        verify(permissionService).requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);
    }

    @Test
    void savingSourceAssignmentsAppliesTheSourceWithoutStartingAnotherBackgroundSync() throws Exception {
        var request = new CodeListEntity().setKey("test");
        var saved = new CodeListEntity().setKey("test");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(new UserEntity().setId("user-1")));
        when(service.updateFromSource("test", request)).thenReturn(saved);

        assertEquals(saved, controller.update(jwt, "test", request, true));

        verify(permissionService).requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);
        verify(service, never()).update(anyString(), any());
        verifyNoInteractions(worker);
    }

    @Test
    void ordinaryUpdatesContinueToScheduleBackgroundSynchronization() throws Exception {
        var request = new CodeListEntity().setKey("test");
        var saved = new CodeListEntity().setKey("test");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(new UserEntity().setId("user-1")));
        when(service.update("test", request)).thenReturn(saved);

        assertEquals(saved, controller.update(jwt, "test", request, false));

        verify(worker).triggerCodeListUpdate("test", true);
        verify(service, never()).updateFromSource(anyString(), any());
    }

    @Test
    void rejectedSourceAssignmentsDoNotStartBackgroundSynchronization() throws Exception {
        var request = new CodeListEntity().setKey("test");
        when(userService.fromJWT(jwt)).thenReturn(Optional.of(new UserEntity().setId("user-1")));
        doThrow(ResponseException.conflict("Quelle geändert")).when(service).updateFromSource("test", request);

        assertThrows(ResponseException.class, () -> controller.update(jwt, "test", request, true));

        verifyNoInteractions(worker);
    }
}
