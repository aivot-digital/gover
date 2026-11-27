package de.aivot.prosuna.backend.config.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemConfigControllerTest {
    private final SystemConfigService systemConfigService = mock(SystemConfigService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ElementDerivationService elementDerivationService = mock(ElementDerivationService.class);
    private final Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("sub", "user-1")
    );

    private SystemConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new SystemConfigController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                systemConfigService,
                mock(UserService.class),
                permissionService,
                elementDerivationService
        );
    }

    @Test
    void deriveCategoryRequiresReadPermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveCategory(jwt, "General", createRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveCategoryDerivesTheConfigElementsOfTheRequestedCategory() throws ResponseException {
        var generalElement = new TextInputElement();
        generalElement.setId("general.name");
        var otherElement = new TextInputElement();
        otherElement.setId("other.name");
        doReturn(List.of(
                mockDefinition("General", generalElement),
                mockDefinition("Other", otherElement)
        )).when(systemConfigService).getSystemConfigDefinitions();
        var derivedData = DerivedRuntimeElementData.empty();
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(derivedData);

        var result = controller.deriveCategory(jwt, "General", createRequest());

        assertSame(derivedData, result);
        var categoryLayout = assertInstanceOf(GroupLayoutElement.class, request.getValue().element());
        assertEquals("General", categoryLayout.getId());
        assertEquals(List.of(generalElement), categoryLayout.getChildren());
    }

    @Test
    void deriveCategoryRejectsUnknownCategories() throws ResponseException {
        doReturn(List.of()).when(systemConfigService).getSystemConfigDefinitions();

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.deriveCategory(jwt, "Unknown", createRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    private static SystemConfigDefinition<?> mockDefinition(String category, TextInputElement configElement) {
        var definition = mock(SystemConfigDefinition.class);
        when(definition.getCategory()).thenReturn(category);
        when(definition.getConfigElement()).thenReturn(configElement);
        return definition;
    }

    private static ElementValuesDerivationRequestDTO createRequest() {
        return new ElementValuesDerivationRequestDTO(new AuthoredElementValues(), new ElementDerivationOptions());
    }
}
