package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Mock
    private HttpService httpService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AuthController(httpService);
        ReflectionTestUtils.setField(controller, "oidcIssuerURI", "https://auth.example.com/realms/gover");
        ReflectionTestUtils.setField(controller, "oidcClientId", "gover-client");
        ReflectionTestUtils.setField(controller, "oidcClientSecret", "gover-secret");
    }

    @Test
    void logoutShouldPerformOidcLogoutAndClearCookies() throws Exception {
        var logoutResponse = mock(HttpResponse.class);
        when(logoutResponse.statusCode()).thenReturn(204);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/realms/gover/protocol/openid-connect/logout")),
                argThat(payload ->
                        "gover-client".equals(payload.get("client_id")) &&
                                "refresh-token".equals(payload.get("refresh_token")) &&
                                "gover-secret".equals(payload.get("client_secret"))
                )
        )).thenReturn(logoutResponse);

        var response = new MockHttpServletResponse();

        controller.logout(response, "refresh-token");

        assertEquals(204, response.getStatus());
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
        verify(httpService).postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/realms/gover/protocol/openid-connect/logout")),
                argThat(payload ->
                        "gover-client".equals(payload.get("client_id")) &&
                                "refresh-token".equals(payload.get("refresh_token")) &&
                                "gover-secret".equals(payload.get("client_secret"))
                )
        );
    }

    @Test
    void logoutShouldClearCookiesWithoutOidcLogoutWhenRefreshCookieIsMissing() throws Exception {
        var response = new MockHttpServletResponse();

        controller.logout(response, null);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
        verifyNoInteractions(httpService);
    }

    @Test
    void logoutShouldClearCookiesWhenOidcLogoutFails() throws Exception {
        var logoutResponse = mock(HttpResponse.class);
        when(logoutResponse.statusCode()).thenReturn(400);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/realms/gover/protocol/openid-connect/logout")),
                argThat(payload -> "refresh-token".equals(payload.get("refresh_token")))
        )).thenReturn(logoutResponse);

        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.logout(response, "refresh-token"));
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
    }

    private static void assertClearsCookie(
            MockHttpServletResponse response,
            String name,
            String path
    ) {
        var cookie = Arrays
                .stream(response.getCookies())
                .filter(candidate -> name.equals(candidate.getName()))
                .filter(candidate -> path.equals(candidate.getPath()))
                .findFirst()
                .orElse(null);

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
