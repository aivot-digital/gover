package de.aivot.prosuna.backend.codeLists.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.prosuna.backend.codeLists.services.CodeListService;
import de.aivot.prosuna.backend.codeLists.services.CodeListWorker;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CodeListControllerTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final CodeListElementOptionsService codeListElementOptionsService = mock(CodeListElementOptionsService.class);
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
                mock(CodeListService.class),
                mock(UserService.class),
                mock(CodeListWorker.class),
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
}
