package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.identity.services.IdentityService;
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

    private IdentityController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new IdentityController(identityService);
    }

    @Test
    void clearSessionShouldDeleteCurrentSessionAndExpireCookie() {
        var response = new MockHttpServletResponse();

        controller.clearSession("identity-session-id", response);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response);
        verify(identityService).clearIdentitySession("identity-session-id");
    }

    @Test
    void clearSessionShouldExpireCookieWhenSessionCookieIsMissing() {
        var response = new MockHttpServletResponse();

        controller.clearSession(null, response);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response);
        verify(identityService).clearIdentitySession(null);
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
