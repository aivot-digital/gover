package de.aivot.prosuna.backend.identity.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.identity.models.IdentityAuthenticationRedirect;
import de.aivot.prosuna.backend.identity.permissions.IdentityProviderPermissionProvider;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.identity.utils.IdentityCookieUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityProviderControllerTest {
    private final Jwt jwt = mock(Jwt.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private IdentityProviderController controller;

    @BeforeEach
    void setUp() {
        controller = new IdentityProviderController(
                mock(AuditService.class, RETURNS_DEEP_STUBS),
                mock(IdentityProviderService.class),
                identityService,
                mock(UserService.class),
                permissionService
        );
    }

    @Test
    void startTestBindsTheAuthenticationToTheBrowser() throws ResponseException {
        var providerKey = UUID.randomUUID();
        var callbackPath = "/api/public/identity/" + providerKey + "/callback/entity-id/";
        when(identityService.createRedirectURL(null, providerKey, providerKey.toString(), "https://example.com/staff", List.of(), 0))
                .thenReturn(new IdentityAuthenticationRedirect(
                        URI.create("https://identity.example.com/authorize"),
                        "flow-secret",
                        callbackPath
                ));
        var response = new MockHttpServletResponse();

        var result = controller.startTest(
                jwt,
                providerKey,
                new IdentityProviderController.IdentityProviderTestStartRequestDTO("https://example.com/staff"),
                response
        );

        assertEquals("https://identity.example.com/authorize", result.redirectUrl());
        var flowBindingCookie = response.getCookie(IdentityCookieUtils.IDENTITY_FLOW_COOKIE_NAME);
        assertNotNull(flowBindingCookie);
        assertEquals("flow-secret", flowBindingCookie.getValue());
        assertEquals(callbackPath, flowBindingCookie.getPath());
    }

    @Test
    void startTestRequiresTheUpdatePermission() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissionService)
                .requireSystemPermission(jwt, IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE);
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.startTest(
                jwt,
                UUID.randomUUID(),
                new IdentityProviderController.IdentityProviderTestStartRequestDTO("https://example.com/staff"),
                response
        ));

        verifyNoInteractions(identityService);
        assertNull(response.getCookie(IdentityCookieUtils.IDENTITY_FLOW_COOKIE_NAME));
    }
}
