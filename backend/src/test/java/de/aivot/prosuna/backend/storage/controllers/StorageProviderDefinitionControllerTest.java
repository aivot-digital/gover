package de.aivot.prosuna.backend.storage.controllers;

import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.permissions.StoragePermissionProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StorageProviderDefinitionControllerTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ElementDerivationService elementDerivationService = mock(ElementDerivationService.class);
    private final Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "user-1")
    );

    @Test
    void definitionDto_ShouldMapAbstractAndDescriptionSeparately() throws Exception {
        @SuppressWarnings("unchecked")
        var definition = (StorageProviderDefinition<Object>) mock(StorageProviderDefinition.class);
        when(definition.getKey()).thenReturn("de.aivot.test.storage");
        when(definition.getMajorVersion()).thenReturn(3);
        when(definition.getName()).thenReturn("Test storage");
        when(definition.getAbstract()).thenReturn("Concise storage abstract.");
        when(definition.getDescription()).thenReturn("Detailed **storage** description.");
        when(definition.getDocumentationUrl()).thenReturn("https://docs.example.com/storage/test");
        when(definition.getSupportsMetadataAttributes()).thenReturn(true);
        when(definition.getProviderConfigLayout()).thenReturn(null);

        var result = StorageProviderDefinitionController.StorageProviderDefinitionDTO.from(definition);

        assertEquals("de.aivot.test.storage", result.key());
        assertEquals(3, result.version());
        assertEquals("Test storage", result.name());
        assertEquals("Concise storage abstract.", result.abstractDescription());
        assertEquals("Detailed **storage** description.", result.description());
        assertEquals("https://docs.example.com/storage/test", result.documentationUrl());
        assertEquals(true, result.supportsMetadataAttributes());
        assertNull(result.providerConfigLayout());
    }

    @Test
    void derive_ShouldRejectUsersWithoutReadOrCreatePermission() throws Exception {
        var controller = createController(mockDefinition(new ConfigLayoutElement()));

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "de.aivot.test.storage", 3, createRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void derive_ShouldDeriveTheDefinitionLayoutWithReadPermission() throws Exception {
        var configLayout = new ConfigLayoutElement();
        var controller = createController(mockDefinition(configLayout));
        when(permissionService.hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ))
                .thenReturn(true);
        var derivedData = DerivedRuntimeElementData.empty();
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(derivedData);

        var result = controller.derive(jwt, "de.aivot.test.storage", 3, createRequest());

        assertSame(derivedData, result);
        assertSame(configLayout, request.getValue().element());
    }

    @Test
    void derive_ShouldDeriveTheDefinitionLayoutWithCreatePermission() throws Exception {
        var configLayout = new ConfigLayoutElement();
        var controller = createController(mockDefinition(configLayout));
        when(permissionService.hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_CREATE))
                .thenReturn(true);
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(DerivedRuntimeElementData.empty());

        controller.derive(jwt, "de.aivot.test.storage", 3, createRequest());

        assertSame(configLayout, request.getValue().element());
    }

    @Test
    void derive_ShouldRejectUnknownDefinitions() throws Exception {
        var controller = createController(mockDefinition(new ConfigLayoutElement()));
        when(permissionService.hasSystemPermission(jwt, StoragePermissionProvider.STORAGE_PROVIDER_READ))
                .thenReturn(true);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "de.aivot.test.storage", 4, createRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    private StorageProviderDefinitionController createController(StorageProviderDefinition<?> definition) {
        return new StorageProviderDefinitionController(
                List.of(definition),
                permissionService,
                elementDerivationService
        );
    }

    private static StorageProviderDefinition<?> mockDefinition(ConfigLayoutElement configLayout) throws ResponseException {
        @SuppressWarnings("unchecked")
        var definition = (StorageProviderDefinition<Object>) mock(StorageProviderDefinition.class);
        when(definition.getKey()).thenReturn("de.aivot.test.storage");
        when(definition.getMajorVersion()).thenReturn(3);
        when(definition.getProviderConfigLayout()).thenReturn(configLayout);
        return definition;
    }

    private static ElementValuesDerivationRequestDTO createRequest() {
        return new ElementValuesDerivationRequestDTO(new AuthoredElementValues(), new ElementDerivationOptions());
    }
}
