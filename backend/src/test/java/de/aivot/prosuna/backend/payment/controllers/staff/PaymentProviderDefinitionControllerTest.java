package de.aivot.prosuna.backend.payment.controllers.staff;

import de.aivot.prosuna.backend.elements.dtos.ElementValuesDerivationRequestDTO;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentProviderDefinitionControllerTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ElementDerivationService elementDerivationService = mock(ElementDerivationService.class);
    private final PaymentProviderDefinition definition = mock(PaymentProviderDefinition.class);
    private final GroupLayoutElement configLayout = new GroupLayoutElement();

    private PaymentProviderDefinitionController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() throws ResponseException {
        when(definition.getKey()).thenReturn("de.aivot.test.payment");
        when(definition.getMajorVersion()).thenReturn(2);
        when(definition.getPaymentConfigLayout()).thenReturn(configLayout);
        controller = new PaymentProviderDefinitionController(
                List.of(definition),
                permissionService,
                elementDerivationService
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
    void deriveRejectsUsersWithoutReadOrCreatePermission() {
        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "de.aivot.test.payment", 2, createRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveWithReadPermissionDerivesTheDefinitionLayout() throws ResponseException {
        when(permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ))
                .thenReturn(true);
        var derivedData = DerivedRuntimeElementData.empty();
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(derivedData);

        var result = controller.derive(jwt, "de.aivot.test.payment", 2, createRequest());

        assertSame(derivedData, result);
        assertSame(configLayout, request.getValue().element());
    }

    @Test
    void deriveWithCreatePermissionDerivesTheDefinitionLayout() throws ResponseException {
        when(permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE))
                .thenReturn(true);
        var request = ArgumentCaptor.forClass(ElementDerivationRequest.class);
        when(elementDerivationService.derive(request.capture())).thenReturn(DerivedRuntimeElementData.empty());

        controller.derive(jwt, "de.aivot.test.payment", 2, createRequest());

        assertSame(configLayout, request.getValue().element());
    }

    @Test
    void deriveRejectsUnknownDefinitions() {
        when(permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ))
                .thenReturn(true);

        var exception = assertThrows(
                ResponseException.class,
                () -> controller.derive(jwt, "de.aivot.test.payment", 3, createRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(elementDerivationService);
    }

    @Test
    void deriveReturnsEmptyDataForDefinitionsWithoutLayout() throws ResponseException {
        when(permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ))
                .thenReturn(true);
        when(definition.getPaymentConfigLayout()).thenReturn(null);

        var result = controller.derive(jwt, "de.aivot.test.payment", 2, createRequest());

        assertTrue(result.getEffectiveValues().isEmpty());
        verifyNoInteractions(elementDerivationService);
    }

    private static ElementValuesDerivationRequestDTO createRequest() {
        return new ElementValuesDerivationRequestDTO(new AuthoredElementValues(), new ElementDerivationOptions());
    }
}
