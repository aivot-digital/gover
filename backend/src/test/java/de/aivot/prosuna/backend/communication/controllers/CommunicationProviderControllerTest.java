package de.aivot.prosuna.backend.communication.controllers;

import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderDefinitionService;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderManagementService;
import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommunicationProviderControllerTest {
    private final CommunicationProviderManagementService managementService = mock(CommunicationProviderManagementService.class);
    private final CommunicationProviderDefinitionService definitionService = mock(CommunicationProviderDefinitionService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private CommunicationProviderController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        controller = new CommunicationProviderController(
                managementService,
                definitionService,
                permissionService
        );
        jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    @Test
    void reorderPassesTheActorAndCompleteOrderToTheService() throws Exception {
        var identityProviderKey = UUID.randomUUID();
        var request = new CommunicationProviderController.BindingOrderRequest(identityProviderKey, List.of(2, 1));
        when(managementService.reorderBindings(jwt, identityProviderKey, request.ids())).thenReturn(List.of());
        controller.reorderBindings(jwt, request);
        verify(managementService).reorderBindings(jwt, identityProviderKey, List.of(2, 1));
    }

    @Test
    void testingLayoutRequiresReadPermissionAndReturnsServiceResult() throws Exception {
        var layout = new GroupLayoutElement();
        when(managementService.getProviderTestingLayout(7)).thenReturn(layout);

        var result = controller.getTestingLayout(jwt, 7);

        assertSame(layout, result);
        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );
        verify(managementService).getProviderTestingLayout(7);
    }

    @Test
    void testRequiresUpdatePermissionAndPassesInputsToService() throws Exception {
        var inputs = new AuthoredElementValues();
        inputs.putLiteral("test-recipient", "customer@example.test");
        var layout = new GroupLayoutElement();
        when(managementService.testProvider(7, inputs)).thenReturn(layout);

        var result = controller.test(jwt, 7, inputs);

        assertSame(layout, result);
        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE
        );
        verify(managementService).testProvider(7, inputs);
    }

    @Test
    void deriveTestingInputsRequiresReadPermissionAndDerivesTheProviderTestingLayout() throws Exception {
        var request = createDerivationRequest();
        var derivedData = DerivedRuntimeElementData.empty();
        when(managementService.deriveProviderTestingInputs(7, request.authoredElementValues(), request.derivationOptions()))
                .thenReturn(derivedData);

        var result = controller.deriveTestingInputs(jwt, 7, request);

        assertSame(derivedData, result);
        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );
    }

    @Test
    void deriveTestingInputsDoesNotDeriveWithoutReadPermission() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );

        assertThrows(ResponseException.class, () -> controller.deriveTestingInputs(jwt, 7, createDerivationRequest()));

        verifyNoInteractions(managementService);
    }

    @Test
    void deriveProviderConfigurationRequiresReadPermissionAndDerivesTheDefinitionLayout() throws Exception {
        var request = createDerivationRequest();
        var derivedData = DerivedRuntimeElementData.empty();
        when(managementService.deriveProviderConfiguration("mail", 1, request.authoredElementValues(), request.derivationOptions()))
                .thenReturn(derivedData);

        var result = controller.deriveProviderConfiguration(jwt, "mail", 1, request);

        assertSame(derivedData, result);
        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );
    }

    @Test
    void deriveProviderConfigurationDoesNotDeriveWithoutReadPermission() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );

        assertThrows(ResponseException.class, () -> controller.deriveProviderConfiguration(jwt, "mail", 1, createDerivationRequest()));

        verifyNoInteractions(managementService);
    }

    @Test
    void deriveBindingConfigurationRequiresReadPermissionAndDerivesTheBindingLayout() throws Exception {
        var identityProviderKey = UUID.randomUUID();
        var request = createDerivationRequest();
        var derivedData = DerivedRuntimeElementData.empty();
        when(managementService.deriveBindingConfiguration(7, identityProviderKey, request.authoredElementValues(), request.derivationOptions()))
                .thenReturn(derivedData);

        var result = controller.deriveBindingConfiguration(jwt, 7, identityProviderKey, request);

        assertSame(derivedData, result);
        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );
    }

    @Test
    void deriveBindingConfigurationDoesNotDeriveWithoutReadPermission() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
        );

        assertThrows(ResponseException.class, () -> controller.deriveBindingConfiguration(jwt, 7, UUID.randomUUID(), createDerivationRequest()));

        verifyNoInteractions(managementService);
    }

    private static ElementValuesDerivationRequestDTO createDerivationRequest() {
        var values = new AuthoredElementValues();
        values.putLiteral("test-recipient", "customer@example.test");
        return new ElementValuesDerivationRequestDTO(values, new ElementDerivationOptions());
    }
}
