package de.aivot.prosuna.backend.identity.controllers;

import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.identity.models.IdentityCallbackResult;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.identity.utils.IdentityCookieUtils;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityControllerTest {
    @Mock
    private IdentityService identityService;
    @Mock
    private IdentityCommunicationService identityCommunicationService;

    private IdentityController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new IdentityController(identityService, identityCommunicationService);
    }

    @Test
    void clearSessionShouldDeleteCurrentSessionAndExpireCookie() {
        var response = new MockHttpServletResponse();

        controller.clearSession("identity-session-id", null, response);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response);
        verify(identityService).clearIdentitySession("identity-session-id", null);
    }

    @Test
    void clearSessionShouldExpireCookieWhenSessionCookieIsMissing() {
        var response = new MockHttpServletResponse();

        controller.clearSession(null, null, response);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response);
        verify(identityService).clearIdentitySession(null, null);
    }

    @Test
    void callbackStoresTheRotatedSessionAndExpiresTheFlowBindingCookie() throws Exception {
        var providerKey = UUID.randomUUID();
        var callbackPath = "/api/public/identity/" + providerKey + "/callback/entity-id/";
        when(identityService.handleCallback(providerKey, "entity-id", "flow-secret", "auth-code", "state"))
                .thenReturn(new IdentityCallbackResult("https://example.com/form?identity-state=0", "rotated-session-id"));
        when(identityService.getCallbackPath(providerKey, "entity-id")).thenReturn(callbackPath);
        var response = new MockHttpServletResponse();

        controller.callback(providerKey, "entity-id", "state", null, null, "auth-code", "flow-secret", response);

        assertEquals("https://example.com/form?identity-state=0", response.getRedirectedUrl());
        var identityCookie = findCookie(response, IdentityController.IDENTITY_COOKIE_NAME, IdentityController.IDENTITY_COOKIE_PATH);
        assertNotNull(identityCookie);
        assertEquals("rotated-session-id", identityCookie.getValue());
        var flowBindingCookie = findCookie(response, IdentityCookieUtils.IDENTITY_FLOW_COOKIE_NAME, callbackPath);
        assertNotNull(flowBindingCookie);
        assertEquals(0, flowBindingCookie.getMaxAge());
    }

    @Test
    void callbackDoesNotStoreASessionWhenTheServiceRejectsTheCallback() throws Exception {
        var providerKey = UUID.randomUUID();
        when(identityService.handleCallback(providerKey, "entity-id", null, "auth-code", "state"))
                .thenThrow(ResponseException.badRequest("Die Anmeldung kann diesem Browser nicht zugeordnet werden. Starten Sie die Anmeldung erneut."));
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () ->
                controller.callback(providerKey, "entity-id", "state", null, null, "auth-code", null, response)
        );

        assertNull(response.getCookie(IdentityController.IDENTITY_COOKIE_NAME));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void errorCallbackExpiresTheFlowBindingCookieWithoutStoringASession() throws Exception {
        var providerKey = UUID.randomUUID();
        var callbackPath = "/api/public/identity/" + providerKey + "/callback/entity-id/";
        when(identityService.createErrorRedirectURL("entity-id", "flow-secret", "state", "access_denied", null))
                .thenReturn("https://example.com/form?identity-state=500");
        when(identityService.getCallbackPath(providerKey, "entity-id")).thenReturn(callbackPath);
        var response = new MockHttpServletResponse();

        controller.callback(providerKey, "entity-id", "state", "access_denied", null, null, "flow-secret", response);

        assertEquals("https://example.com/form?identity-state=500", response.getRedirectedUrl());
        assertNull(response.getCookie(IdentityController.IDENTITY_COOKIE_NAME));
        var flowBindingCookie = findCookie(response, IdentityCookieUtils.IDENTITY_FLOW_COOKIE_NAME, callbackPath);
        assertNotNull(flowBindingCookie);
        assertEquals(0, flowBindingCookie.getMaxAge());
        verify(identityService, never()).handleCallback(any(), anyString(), any(), any(), anyString());
    }

    private static Cookie findCookie(MockHttpServletResponse response, String name, String path) {
        return Arrays
                .stream(response.getCookies())
                .filter(candidate -> name.equals(candidate.getName()))
                .filter(candidate -> path.equals(candidate.getPath()))
                .findFirst()
                .orElse(null);
    }

    private static void assertClearsCookie(MockHttpServletResponse response) {
        var cookie = Arrays
                .stream(response.getCookies())
                .filter(candidate -> IdentityController.IDENTITY_COOKIE_NAME.equals(candidate.getName()))
                .filter(candidate -> IdentityController.IDENTITY_COOKIE_PATH.equals(candidate.getPath()))
                .findFirst()
                .orElse(null);

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
