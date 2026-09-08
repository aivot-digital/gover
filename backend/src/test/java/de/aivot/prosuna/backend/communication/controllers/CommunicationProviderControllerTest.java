package de.aivot.prosuna.backend.communication.controllers;

import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderDefinitionService;
import de.aivot.prosuna.backend.communication.services.CommunicationProviderManagementService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        inputs.put("test-recipient", "customer@example.test");

        controller.test(jwt, 7, inputs);

        verify(permissionService).requireSystemPermission(
                jwt,
                CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE
        );
        verify(managementService).testProvider(7, inputs);
    }
}
