package de.aivot.gover.backend.system.controllers;

import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.system.controllers.AuthController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Mock
    private HttpService httpService;
    @Mock
    private CsrfTokenRepository csrfTokenRepository;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redis.opsForValue()).thenReturn(valueOperations);
        controller = new AuthController(httpService, csrfTokenRepository, redis);
        ReflectionTestUtils.setField(controller, "hostname", "https://gover.example.com");
        ReflectionTestUtils.setField(controller, "oidcHostname", "https://auth.example.com");
        ReflectionTestUtils.setField(controller, "oidcInternalHostname", "https://auth.example.com");
        ReflectionTestUtils.setField(controller, "oidcRealm", "gover");
        ReflectionTestUtils.setField(controller, "oidcClientId", "gover-client");
        ReflectionTestUtils.setField(controller, "oidcClientSecret", "gover-secret");
    }

    @Test
    void loginShouldStartPkceFlowWithoutSendingClientSecretToBrowser() throws Exception {
        var request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");
        var response = new MockHttpServletResponse();

        controller.login(request, response, "/staff/dashboard");

        var location = response.getRedirectedUrl();
        assertNotNull(location);
        assertFalse(location.contains("client_secret"));

        var params = UriComponentsBuilder
                .fromUriString(location)
                .build()
                .getQueryParams();
        var state = params.getFirst("state");
        assertNotNull(state);
        assertFalse(state.isBlank());
        assertEquals("gover-client", params.getFirst("client_id"));
        assertEquals("https://gover.example.com/api/auth/oidc-callback", params.getFirst("redirect_uri"));
        assertEquals("code", params.getFirst("response_type"));
        assertEquals("openid profile email", URLDecoder.decode(params.getFirst("scope"), StandardCharsets.UTF_8));
        assertEquals("S256", params.getFirst("code_challenge_method"));
        assertNotNull(params.getFirst("code_challenge"));

        var authFlowCookie = findCookie(response, AuthController.AUTH_FLOW_COOKIE_NAME, "/api/auth/");
        assertNotNull(authFlowCookie);
        assertEquals(state, authFlowCookie.getValue());
        assertEquals(600, authFlowCookie.getMaxAge());
        assertTrue(authFlowCookie.getSecure());
        assertTrue(authFlowCookie.isHttpOnly());
        assertEquals("Lax", authFlowCookie.getAttribute("SameSite"));

        verify(valueOperations).set(
                eq("auth:pkce:" + state),
                argThat(value ->
                        value.contains("\"redirectUri\":\"https://gover.example.com/api/auth/oidc-callback\"") &&
                                value.contains("\"appUri\":\"/staff/dashboard\"") &&
                                value.contains("\"codeVerifier\":")
                ),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void loginShouldAllowGoverHostnameAppRedirectOrigin() throws Exception {
        var request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");
        var response = new MockHttpServletResponse();

        controller.login(request, response, "https://gover.example.com/staff/dashboard");

        assertNotNull(response.getRedirectedUrl());
        verify(valueOperations).set(
                anyString(),
                argThat(value -> value.contains("\"appUri\":\"https://gover.example.com/staff/dashboard\"")),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void loginShouldRejectDisallowedAppRedirectBeforeStoringState() {
        var request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.login(request, response, "https://evil.example/dashboard"));

        assertNull(response.getRedirectedUrl());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        verifyNoInteractions(httpService);
    }

    @Test
    void callbackShouldValidateStateAndUseCodeVerifierWithClientSecret() throws Exception {
        when(valueOperations.getAndDelete("auth:pkce:state")).thenReturn("""
                {"codeVerifier":"verifier","redirectUri":"https://gover.example.com/api/auth/oidc-callback","appUri":"/staff/dashboard"}
                """);

        var tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("""
                {"access_token":"access-token","refresh_token":"refresh-token","expires_in":60,"refresh_expires_in":120}
                """);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/realms/gover/protocol/openid-connect/token")),
                argThat(payload ->
                        "authorization_code".equals(payload.get("grant_type")) &&
                                "gover-client".equals(payload.get("client_id")) &&
                                "gover-secret".equals(payload.get("client_secret")) &&
                                "authorization-code".equals(payload.get("code")) &&
                                "https://gover.example.com/api/auth/oidc-callback".equals(payload.get("redirect_uri")) &&
                                "verifier".equals(payload.get("code_verifier"))
                )
        )).thenReturn(tokenResponse);

        var response = new MockHttpServletResponse();

        controller.idpCallback(response, "state", "authorization-code", "state");

        assertEquals("/staff/dashboard", response.getRedirectedUrl());
        assertNotNull(findCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/"));
        assertNotNull(findCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/"));
        assertClearsCookie(response, AuthController.AUTH_FLOW_COOKIE_NAME, "/api/auth/");
    }

    @Test
    void callbackShouldRejectDisallowedStoredAppRedirectWithoutTokenRequest() {
        when(valueOperations.getAndDelete("auth:pkce:state")).thenReturn("""
                {"codeVerifier":"verifier","redirectUri":"https://gover.example.com/api/auth/oidc-callback","appUri":"https://evil.example/dashboard"}
                """);

        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.idpCallback(response, "state", "authorization-code", "state"));

        verifyNoInteractions(httpService);
        assertClearsCookie(response, AuthController.AUTH_FLOW_COOKIE_NAME, "/api/auth/");
    }

    @Test
    void callbackShouldRejectMismatchedStateWithoutTokenRequest() {
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.idpCallback(response, "state", "code", "other-state"));

        verify(valueOperations, never()).getAndDelete(anyString());
        verifyNoInteractions(httpService);
        assertClearsCookie(response, AuthController.AUTH_FLOW_COOKIE_NAME, "/api/auth/");
    }

    @Test
    void callbackShouldRejectExpiredStateWithoutTokenRequest() {
        when(valueOperations.getAndDelete("auth:pkce:state")).thenReturn(null);

        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.idpCallback(response, "state", "code", "state"));

        verifyNoInteractions(httpService);
        assertClearsCookie(response, AuthController.AUTH_FLOW_COOKIE_NAME, "/api/auth/");
    }

    @Test
    void refreshShouldUseClientSecret() throws Exception {
        var tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("""
                {"access_token":"access-token","refresh_token":"refresh-token","expires_in":60,"refresh_expires_in":120}
                """);
        when(httpService.postFormUrlEncoded(
                eq(URI.create("https://auth.example.com/realms/gover/protocol/openid-connect/token")),
                argThat(payload ->
                        "refresh_token".equals(payload.get("grant_type")) &&
                                "gover-client".equals(payload.get("client_id")) &&
                                "gover-secret".equals(payload.get("client_secret")) &&
                                "refresh-token".equals(payload.get("refresh_token"))
                )
        )).thenReturn(tokenResponse);

        var response = new MockHttpServletResponse();

        var authStatus = controller.refresh(response, "refresh-token");

        assertNotNull(authStatus);
        assertNotNull(findCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/"));
        assertNotNull(findCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/"));
    }

    @Test
    void createCodeChallengeShouldUseS256() throws Exception {
        var challenge = AuthController.createCodeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");

        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge);
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

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        controller.logout(request, response, "refresh-token");

        assertEquals(204, response.getStatus());
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
        verify(csrfTokenRepository).saveToken(null, request, response);
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
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        controller.logout(request, response, null);

        assertEquals(204, response.getStatus());
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
        verify(csrfTokenRepository).saveToken(null, request, response);
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

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> controller.logout(request, response, "refresh-token"));
        assertClearsCookie(response, AuthController.ACCESS_COOKIE_NAME, "/api/");
        assertClearsCookie(response, AuthController.REFRESH_COOKIE_NAME, "/api/auth/");
        verify(csrfTokenRepository).saveToken(null, request, response);
    }

    private static Cookie findCookie(
            MockHttpServletResponse response,
            String name,
            String path
    ) {
        return Arrays
                .stream(response.getCookies())
                .filter(candidate -> name.equals(candidate.getName()))
                .filter(candidate -> path.equals(candidate.getPath()))
                .findFirst()
                .orElse(null);
    }

    private static void assertClearsCookie(
            MockHttpServletResponse response,
            String name,
            String path
    ) {
        var cookie = findCookie(response, name, path);

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
