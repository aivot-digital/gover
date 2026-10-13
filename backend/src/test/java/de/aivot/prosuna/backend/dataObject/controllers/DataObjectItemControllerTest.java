package de.aivot.prosuna.backend.dataObject.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.prosuna.backend.dataObject.services.DataObjectItemService;
import de.aivot.prosuna.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataObjectItemControllerTest {
    private final DataObjectItemService itemService = mock(DataObjectItemService.class);
    private final DataObjectSchemaService schemaService = mock(DataObjectSchemaService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final DataObjectSchemaEntity schema = new DataObjectSchemaEntity().setKey("contacts");
    private final Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "user-1")
    );

    private DataObjectItemController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        when(schemaService.retrieve("contacts")).thenReturn(Optional.of(schema));
        controller = new DataObjectItemController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                itemService,
                schemaService,
                mock(UserService.class),
                permissionService
        );
    }

    @Test
    void deriveNewRequiresCreatePermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_ITEM_CREATE);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveNew(jwt, "contacts", createRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(itemService, never()).deriveItemEditorData(any(), anyBoolean(), any(), any());
    }

    @Test
    void deriveNewDerivesTheStoredSchemaForANewItem() throws ResponseException {
        var request = createRequest();
        var derivedData = DerivedRuntimeElementData.empty();
        when(itemService.deriveItemEditorData(schema, false, request.authoredElementValues(), request.derivationOptions()))
                .thenReturn(derivedData);

        var result = controller.deriveNew(jwt, "contacts", request);

        assertSame(derivedData, result);
    }

    @Test
    void deriveRequiresReadPermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_ITEM_READ);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "contacts", "contact-1", createRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(itemService, never()).deriveItemEditorData(any(), anyBoolean(), any(), any());
    }

    @Test
    void deriveDerivesTheStoredSchemaForAnExistingItem() throws ResponseException {
        when(itemService.exists(new DataObjectItemEntityId("contacts", "contact-1"))).thenReturn(true);
        var request = createRequest();
        var derivedData = DerivedRuntimeElementData.empty();
        when(itemService.deriveItemEditorData(schema, true, request.authoredElementValues(), request.derivationOptions()))
                .thenReturn(derivedData);

        var result = controller.derive(jwt, "contacts", "contact-1", request);

        assertSame(derivedData, result);
    }

    @Test
    void deriveRejectsUnknownItems() throws ResponseException {
        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "contacts", "unknown", createRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(itemService, never()).deriveItemEditorData(any(), anyBoolean(), any(), any());
    }

    private static ElementValuesDerivationRequestDTO createRequest() {
        return new ElementValuesDerivationRequestDTO(new AuthoredElementValues(), new ElementDerivationOptions());
    }
}
