package de.aivot.prosuna.backend.identity.controllers;

import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

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
