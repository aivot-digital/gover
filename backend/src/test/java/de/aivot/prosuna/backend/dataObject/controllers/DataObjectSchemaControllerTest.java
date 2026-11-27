package de.aivot.prosuna.backend.dataObject.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.prosuna.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.prosuna.backend.elements.dtos.ElementDraftDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataObjectSchemaControllerTest {
    private final DataObjectSchemaService schemaService = mock(DataObjectSchemaService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ElementDerivationService elementDerivationService = mock(ElementDerivationService.class);
    private final Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "user-1")
    );

    private DataObjectSchemaController controller;

    @BeforeEach
    void setUp() {
        controller = new DataObjectSchemaController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                schemaService,
                mock(UserService.class),
                permissionService,
                elementDerivationService
        );
    }

    @Test
    void deriveNewRequiresCreatePermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_CREATE);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveNew(jwt, createRequest(new GroupLayoutElement()))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveNewDerivesTheUnsavedSchemaWithCreatePermission() throws ResponseException {
        var unsavedSchema = new GroupLayoutElement();
        var derivedData = DerivedRuntimeElementData.empty();
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(derivedData);

        var result = controller.deriveNew(jwt, createRequest(unsavedSchema));

        assertSame(derivedData, result);
        assertSame(unsavedSchema, request.getValue().element());
    }

    @Test
    void deriveNewRequiresTheUnsavedSchema() {
        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveNew(jwt, createRequest(null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveRequiresUpdatePermissionForUnsavedSchemas() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "contacts", createRequest(new GroupLayoutElement()))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(permissionService, never())
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_READ);
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveDerivesTheUnsavedSchemaWithUpdatePermission() throws ResponseException {
        when(schemaService.retrieve("contacts")).thenReturn(Optional.of(createStoredSchema()));
        var unsavedSchema = new GroupLayoutElement();
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(DerivedRuntimeElementData.empty());

        controller.derive(jwt, "contacts", createRequest(unsavedSchema));

        verify(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE);
        assertSame(unsavedSchema, request.getValue().element());
    }

    @Test
    void deriveDerivesTheStoredSchemaWithReadPermission() throws ResponseException {
        var storedSchema = createStoredSchema();
        when(schemaService.retrieve("contacts")).thenReturn(Optional.of(storedSchema));
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(DerivedRuntimeElementData.empty());

        controller.derive(jwt, "contacts", createRequest(null));

        verify(permissionService)
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_READ);
        verify(permissionService, never())
                .requireSystemPermission(jwt, DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE);
        assertSame(storedSchema.getSchema(), request.getValue().element());
    }

    @Test
    void deriveRejectsUnsavedSchemasThatAreNoGroupLayout() throws ResponseException {
        when(schemaService.retrieve("contacts")).thenReturn(Optional.of(createStoredSchema()));

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "contacts", createRequest(new ConfigLayoutElement()))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    private static DataObjectSchemaEntity createStoredSchema() {
        var schema = new GroupLayoutElement();
        schema.setId("stored");
        return new DataObjectSchemaEntity()
                .setKey("contacts")
                .setSchema(schema);
    }

    private static ElementDraftDerivationRequestDTO createRequest(BaseElement element) {
        return new ElementDraftDerivationRequestDTO(element, new AuthoredElementValues(), new ElementDerivationOptions());
    }
}
