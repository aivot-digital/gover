package de.aivot.prosuna.backend.system.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.utils.RandomUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/auth/")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {
    private static final String AUTH_PATH = "/protocol/openid-connect/auth";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";
    private static final String LOGOUT_PATH = "/protocol/openid-connect/logout";

    private static final String APP_URI_QUERY_PARAM = "app_uri";

    private static final String OIDC_CLIENT_ID_PARAM_KEY = "client_id";
    private static final String OIDC_CLIENT_SECRET_PARAM_KEY = "client_secret";
    private static final String OIDC_RESPONSE_TYPE_PARAM_KEY = "response_type";
    private static final String OIDC_RESPONSE_TYPE_VALUE = "code";
    private static final String OIDC_RESPONSE_SCOPE_PARAM_KEY = "scope";
    private static final String OIDC_RESPONSE_SCOPE_PARAM_VALUE = "openid profile email";
    private static final String OIDC_REDIRECT_URI_PARAM_KEY = "redirect_uri";
    private static final String OIDC_STATE_PARAM_KEY = "state";
    private static final String OIDC_CODE_CHALLENGE_PARAM_KEY = "code_challenge";
    private static final String OIDC_CODE_CHALLENGE_METHOD_PARAM_KEY = "code_challenge_method";
    private static final String OIDC_CODE_CHALLENGE_METHOD_VALUE = "S256";
    private static final String OIDC_CODE_VERIFIER_PARAM_KEY = "code_verifier";

    private static final String OIDC_GRANT_TYPE_PARAM_KEY = "grant_type";
    private static final String OIDC_GRANT_TYPE_VALUE = "authorization_code";
    private static final String OIDC_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String OIDC_REFRESH_TOKEN_PARAM_KEY = "refresh_token";

    private static final String OIDC_CALLBACK_CODE_PARAM_KEY = "code";
    private static final String DEFAULT_APP_URI = "/staff";
    private static final String CALLBACK_ERROR_VIEW = "auth/oidc-callback-error";
    private static final String MISSING_AUTHORIZATION_CODE_MESSAGE = "Es wurde kein Autorisierungscode übergeben.";
    private static final String INVALID_STATE_MESSAGE = "Der state-Parameter ist ungültig.";
    private static final String EXPIRED_AUTH_FLOW_MESSAGE = "Die Authentifizierungssitzung ist abgelaufen.";
    private static final String INVALID_APP_REDIRECT_MESSAGE = "Die App-Weiterleitungsadresse ist ungültig.";
    private static final String DISALLOWED_APP_REDIRECT_MESSAGE = "Die App-Weiterleitungsadresse ist nicht erlaubt.";

    private static final String MISSING_AUTHORIZATION_CODE_DESCRIPTION = "Der Identitätsanbieter hat keinen Autorisierungscode zurückgegeben. Ohne diesen Code kann Prosuna keine Sitzung erstellen. Starten Sie die Anmeldung erneut.";
    private static final String INVALID_STATE_DESCRIPTION = "Die Sicherheitsprüfung der Anmeldung ist fehlgeschlagen. Das kann passieren, wenn der Link aus einem alten Browser-Tab stammt, Cookies fehlen oder parallel eine neue Anmeldung gestartet wurde.";
    private static final String EXPIRED_AUTH_FLOW_DESCRIPTION = "Ihre Anmeldesitzung ist abgelaufen. Starten Sie die Anmeldung erneut, damit eine neue sichere Sitzung erstellt wird.";
    private static final String INVALID_APP_REDIRECT_DESCRIPTION = "Die gespeicherte Rücksprungadresse der Anwendung ist ungültig oder nicht erlaubt. Die Anmeldung kann über den Standardbereich neu gestartet werden.";
    private static final String TOKEN_EXCHANGE_ERROR_DESCRIPTION = "Der Identitätsanbieter hat geantwortet, aber Prosuna konnte die Anmeldung nicht abschließen. Bitte versuchen Sie es später erneut oder wenden Sie sich an den Support, falls der Fehler bestehen bleibt.";

    public static final String ACCESS_COOKIE_NAME = "access";
    public static final String REFRESH_COOKIE_NAME = "refresh";
    public static final String AUTH_FLOW_COOKIE_NAME = "auth_flow";
    private static final String ACCESS_COOKIE_PATH = "/api/";
    private static final String REFRESH_COOKIE_PATH = "/api/auth/";
    private static final String AUTH_FLOW_COOKIE_PATH = "/api/auth/";
    private static final String AUTH_FLOW_REDIS_PREFIX = "auth:pkce:";
    private static final Duration AUTH_FLOW_TTL = Duration.ofMinutes(10);

    @Value("${prosuna.prosunaHostname}")
    private String hostname;

    @Value("${keycloak.hostname}")
    private String oidcHostname;

    @Value("${keycloak.internalHostname}")
    private String oidcInternalHostname;

    @Value("${keycloak.realm}")
    private String oidcRealm;

    @Value("${keycloak.frontendClientId}")
    private String oidcClientId;

    @Value("${keycloak.frontendClientSecret}")
    private String oidcClientSecret;

    private final HttpService httpService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final StringRedisTemplate redis;

    public AuthController(
            HttpService httpService,
            CsrfTokenRepository csrfTokenRepository,
            StringRedisTemplate redis
    ) {
        this.httpService = httpService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.redis = redis;
    }

    private String getIssuerURI() {
        return getIssuerURI(false);
    }

    private String getIssuerURI(boolean internal) {
        return UriComponentsBuilder
                .fromUriString(internal ? oidcInternalHostname : oidcHostname)
                .path("/realms/")
                .path(oidcRealm)
                .build()
                .toUriString();
    }

    @GetMapping("login")
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public void login(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull @RequestParam(value = APP_URI_QUERY_PARAM) String appUri
    ) throws IOException, ResponseException {
        var appRedirectLocation = resolveAppRedirectLocation(appUri);

        var callbackRedirectUri = UriComponentsBuilder
                .fromUriString(hostname)
                .path(request.getServletPath().replace("/login", "/oidc-callback"))
                .toUriString();

        var state = generateOpaqueValue();
        var codeVerifier = generateOpaqueValue();
        var codeChallenge = createCodeChallenge(codeVerifier);

        saveAuthFlowState(
                state,
                new AuthFlowState(
                        codeVerifier,
                        callbackRedirectUri,
                        appRedirectLocation
                )
        );
        response.addCookie(getAuthFlowCookie(state));

        var uriBuilder = UriComponentsBuilder
                .fromUriString(getIssuerURI())
                .path(AUTH_PATH)
                .queryParam(OIDC_CLIENT_ID_PARAM_KEY, oidcClientId)
                .queryParam(OIDC_REDIRECT_URI_PARAM_KEY, callbackRedirectUri)
                .queryParam(OIDC_RESPONSE_TYPE_PARAM_KEY, OIDC_RESPONSE_TYPE_VALUE)
                .queryParam(OIDC_RESPONSE_SCOPE_PARAM_KEY, OIDC_RESPONSE_SCOPE_PARAM_VALUE)
                .queryParam(OIDC_STATE_PARAM_KEY, state)
                .queryParam(OIDC_CODE_CHALLENGE_PARAM_KEY, codeChallenge)
                .queryParam(OIDC_CODE_CHALLENGE_METHOD_PARAM_KEY, OIDC_CODE_CHALLENGE_METHOD_VALUE);

        response.sendRedirect(uriBuilder.toUriString());
    }

    @GetMapping("refresh")
    @ResponseBody
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public AuthStatusResponse refresh(
            @Nonnull HttpServletResponse response,
            @Nonnull @CookieValue(value = REFRESH_COOKIE_NAME) String refreshToken
    ) throws ResponseException {
        var payload = new HashMap<String, String>();
        payload.put(OIDC_GRANT_TYPE_PARAM_KEY, OIDC_GRANT_TYPE_REFRESH_TOKEN);
        payload.put(OIDC_CLIENT_ID_PARAM_KEY, oidcClientId);
        payload.put(OIDC_REFRESH_TOKEN_PARAM_KEY, refreshToken);

        if (StringUtils.isNotNullOrEmpty(oidcClientSecret)) {
            payload.put(OIDC_CLIENT_SECRET_PARAM_KEY, oidcClientSecret);
        }

        var tokenResponse = getTokenResponse(payload);

        response
                .addCookie(getRefreshCookie(tokenResponse));
        response
                .addCookie(getAccessCookie(tokenResponse));

        return AuthStatusResponse.of(tokenResponse);
    }


    @GetMapping("oidc-callback")
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public ModelAndView idpCallback(
            @Nonnull HttpServletResponse response,
            @Nullable @RequestParam(value = OIDC_STATE_PARAM_KEY, required = false) String state,
            @Nullable @RequestParam(value = OIDC_CALLBACK_CODE_PARAM_KEY, required = false) String code,
            @Nullable @CookieValue(value = AUTH_FLOW_COOKIE_NAME, required = false) String authFlowState
    ) {
        try {
            if (StringUtils.isNullOrEmpty(code)) {
                return getIdpCallbackErrorView(
                        response,
                        ResponseException.badRequest(MISSING_AUTHORIZATION_CODE_MESSAGE),
                        MISSING_AUTHORIZATION_CODE_DESCRIPTION,
                        getRestartLoginUrl(getAppUriForRestart(state, authFlowState))
                );
            }

            AuthFlowState flowState;
            try {
                flowState = consumeAuthFlowState(state, authFlowState);
            } catch (ResponseException e) {
                return getIdpCallbackErrorView(response, e, getStateErrorDescription(e), getRestartLoginUrl(DEFAULT_APP_URI));
            }

            String appRedirectLocation;
            try {
                appRedirectLocation = resolveAppRedirectLocation(flowState.appUri);
            } catch (ResponseException e) {
                return getIdpCallbackErrorView(response, e, INVALID_APP_REDIRECT_DESCRIPTION, getRestartLoginUrl(DEFAULT_APP_URI));
            }

            var payload = new HashMap<String, String>();
            payload.put(OIDC_GRANT_TYPE_PARAM_KEY, OIDC_GRANT_TYPE_VALUE);
            payload.put(OIDC_CLIENT_ID_PARAM_KEY, oidcClientId);
            payload.put(OIDC_CALLBACK_CODE_PARAM_KEY, code);
            payload.put(OIDC_REDIRECT_URI_PARAM_KEY, flowState.redirectUri);
            payload.put(OIDC_RESPONSE_SCOPE_PARAM_KEY, OIDC_RESPONSE_SCOPE_PARAM_VALUE);
            payload.put(OIDC_CODE_VERIFIER_PARAM_KEY, flowState.codeVerifier);

            if (StringUtils.isNotNullOrEmpty(oidcClientSecret)) {
                payload.put(OIDC_CLIENT_SECRET_PARAM_KEY, oidcClientSecret);
            }

            TokenResponse tokenResponse;
            try {
                tokenResponse = getTokenResponse(payload);
            } catch (ResponseException e) {
                return getIdpCallbackErrorView(response, e, TOKEN_EXCHANGE_ERROR_DESCRIPTION, null);
            }

            var refreshCookie = getRefreshCookie(tokenResponse);
            response.addCookie(refreshCookie);

            var accessCookie = getAccessCookie(tokenResponse);
            response.addCookie(accessCookie);

            return new ModelAndView("redirect:" + appRedirectLocation);
        } finally {
            response.addCookie(getExpiredAuthFlowCookie());
        }
    }

    @PostMapping("logout")
    @Operation(
            summary = "Logout",
            description = "Terminates the authentication provider session and clears local authentication cookies."
    )
    public void logout(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nullable @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) throws ResponseException {
        try {
            if (StringUtils.isNotNullOrEmpty(refreshToken)) {
                performOidcLogout(refreshToken);
            }
        } finally {
            csrfTokenRepository.saveToken(null, request, response);
            response.addCookie(getExpiredAccessCookie());
            response.addCookie(getExpiredRefreshCookie(REFRESH_COOKIE_PATH));
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Nonnull
    private TokenResponse getTokenResponse(@Nonnull Map<String, String> payload) throws ResponseException {
        var tokenUri = UriComponentsBuilder
                .fromUriString(getIssuerURI(true))
                .path(TOKEN_PATH)
                .build()
                .toUri();

        HttpResponse<String> res;
        try {
            res = httpService
                    .postFormUrlEncoded(tokenUri, payload);
        } catch (HttpConnectionException e) {
            throw ResponseException.internalServerError(e, "Failed to exchange authorization code for access token: " + e.getMessage());
        }

        if (res.statusCode() != 200) {
            throw ResponseException.internalServerError("Failed to exchange authorization code for access token. status code: " + res.statusCode());
        }

        TokenResponse tokenResponse;
        try {
            tokenResponse = ObjectMapperFactory
                    .getInstance()
                    .readValue(res.body(), TokenResponse.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e, "Failed to parse access token response: " + e.getMessage());
        }
        return tokenResponse;
    }

    private void saveAuthFlowState(
            @Nonnull String state,
            @Nonnull AuthFlowState authFlowState
    ) throws ResponseException {
        try {
            var value = ObjectMapperFactory
                    .getInstance()
                    .writeValueAsString(authFlowState);
            redis
                    .opsForValue()
                    .set(getAuthFlowRedisKey(state), value, AUTH_FLOW_TTL);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e, "Failed to create auth flow state: " + e.getMessage());
        }
    }

    @Nonnull
    private ModelAndView getIdpCallbackErrorView(
            @Nonnull HttpServletResponse response,
            @Nonnull ResponseException exception,
            @Nonnull String description,
            @Nullable String restartLoginUrl
    ) {
        response.setStatus(exception.getStatus().value());

        var modelAndView = new ModelAndView(CALLBACK_ERROR_VIEW);
        modelAndView.setStatus(exception.getStatus());
        modelAndView.addObject("message", exception.getTitle());
        modelAndView.addObject("description", description);

        if (StringUtils.isNotNullOrEmpty(restartLoginUrl)) {
            modelAndView.addObject("restartLoginUrl", restartLoginUrl);
        }

        return modelAndView;
    }

    @Nonnull
    private static String getStateErrorDescription(@Nonnull ResponseException exception) {
        return EXPIRED_AUTH_FLOW_MESSAGE.equals(exception.getTitle()) ? EXPIRED_AUTH_FLOW_DESCRIPTION : INVALID_STATE_DESCRIPTION;
    }

    @Nonnull
    private String getAppUriForRestart(
            @Nullable String state,
            @Nullable String authFlowStateCookie
    ) {
        if (StringUtils.isNullOrEmpty(state) || StringUtils.isNullOrEmpty(authFlowStateCookie) || !state.equals(authFlowStateCookie)) {
            return DEFAULT_APP_URI;
        }

        var value = redis
                .opsForValue()
                .get(getAuthFlowRedisKey(state));

        if (value == null) {
            return DEFAULT_APP_URI;
        }

        try {
            var flowState = ObjectMapperFactory
                    .getInstance()
                    .readValue(value, AuthFlowState.class);
            return resolveAppRedirectLocation(flowState.appUri);
        } catch (JsonProcessingException | ResponseException e) {
            return DEFAULT_APP_URI;
        }
    }

    @Nonnull
    private static String getRestartLoginUrl(@Nonnull String appUri) {
        return UriComponentsBuilder
                .fromPath(AUTH_FLOW_COOKIE_PATH)
                .path("login")
                .queryParam(APP_URI_QUERY_PARAM, appUri)
                .build()
                .encode()
                .toUriString();
    }

    @Nonnull
    private AuthFlowState consumeAuthFlowState(
            @Nullable String state,
            @Nullable String authFlowStateCookie
    ) throws ResponseException {
        if (StringUtils.isNullOrEmpty(state) || StringUtils.isNullOrEmpty(authFlowStateCookie)) {
            throw ResponseException.badRequest(INVALID_STATE_MESSAGE);
        }

        if (!state.equals(authFlowStateCookie)) {
            throw ResponseException.badRequest(INVALID_STATE_MESSAGE);
        }

        var value = redis
                .opsForValue()
                .getAndDelete(getAuthFlowRedisKey(state));

        if (value == null) {
            throw ResponseException.badRequest(EXPIRED_AUTH_FLOW_MESSAGE);
        }

        try {
            return ObjectMapperFactory
                    .getInstance()
                    .readValue(value, AuthFlowState.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e, "Failed to parse auth flow state: " + e.getMessage());
        }
    }

    @Nonnull
    private static String getAuthFlowRedisKey(@Nonnull String state) {
        return AUTH_FLOW_REDIS_PREFIX + state;
    }

    @Nonnull
    private String resolveAppRedirectLocation(@Nonnull String appUri) throws ResponseException {
        URI appRedirectUri;
        try {
            appRedirectUri = new URI(appUri);
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw ResponseException.badRequest(INVALID_APP_REDIRECT_MESSAGE);
        }

        if (!appRedirectUri.isAbsolute()) {
            if (appUri.startsWith("/") && !appUri.startsWith("//") && appRedirectUri.getRawAuthority() == null) {
                return appRedirectUri.toString();
            }
            throw ResponseException.badRequest(INVALID_APP_REDIRECT_MESSAGE);
        }

        if (!hasAllowedSchemeAndHost(appRedirectUri)) {
            throw ResponseException.badRequest(INVALID_APP_REDIRECT_MESSAGE);
        }

        if (!hasSameOrigin(appRedirectUri, parseConfiguredAppRedirectOrigin(hostname))) {
            throw ResponseException.badRequest(DISALLOWED_APP_REDIRECT_MESSAGE);
        }

        return appRedirectUri.toString();
    }

    @Nonnull
    private URI parseConfiguredAppRedirectOrigin(@Nonnull String configuredOrigin) throws ResponseException {
        try {
            var uri = new URI(configuredOrigin.trim());
            if (hasAllowedSchemeAndHost(uri)) {
                return uri;
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
        }

        throw ResponseException.internalServerError("Eine konfigurierte App-Weiterleitungsadresse ist ungültig.");
    }

    private static boolean hasAllowedSchemeAndHost(@Nonnull URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
    }

    private static boolean hasSameOrigin(@Nonnull URI uri, @Nonnull URI allowedOrigin) {
        return uri.getScheme().equalsIgnoreCase(allowedOrigin.getScheme())
                && uri.getHost().equalsIgnoreCase(allowedOrigin.getHost())
                && getOriginPort(uri) == getOriginPort(allowedOrigin);
    }

    private static int getOriginPort(@Nonnull URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return -1;
    }

    private void performOidcLogout(@Nonnull String refreshToken) throws ResponseException {
        var logoutUri = UriComponentsBuilder
                .fromUriString(getIssuerURI(true))
                .path(LOGOUT_PATH)
                .build()
                .toUri();

        var payload = new HashMap<String, String>();
        payload.put(OIDC_CLIENT_ID_PARAM_KEY, oidcClientId);
        payload.put(OIDC_REFRESH_TOKEN_PARAM_KEY, refreshToken);

        if (StringUtils.isNotNullOrEmpty(oidcClientSecret)) {
            payload.put(OIDC_CLIENT_SECRET_PARAM_KEY, oidcClientSecret);
        }

        HttpResponse<String> res;
        try {
            res = httpService
                    .postFormUrlEncoded(logoutUri, payload);
        } catch (HttpConnectionException e) {
            throw ResponseException.internalServerError(e, "Failed to perform OIDC logout: " + e.getMessage());
        }

        if (res.statusCode() >= 400) {
            throw ResponseException.internalServerError("Failed to perform OIDC logout");
        }
    }

    @Nonnull
    private static Cookie getAccessCookie(TokenResponse tokenResponse) {
        var accessCookie = new Cookie(ACCESS_COOKIE_NAME, tokenResponse.access_token);
        accessCookie.setSecure(true);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath(ACCESS_COOKIE_PATH);
        accessCookie.setMaxAge(tokenResponse.expires_in);
        accessCookie.setAttribute("SameSite", "Strict");
        return accessCookie;
    }

    @Nonnull
    private static Cookie getRefreshCookie(TokenResponse tokenResponse) {
        var refreshCookie = new Cookie(REFRESH_COOKIE_NAME, tokenResponse.refresh_token);
        refreshCookie.setSecure(true);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath(REFRESH_COOKIE_PATH);
        refreshCookie.setMaxAge(tokenResponse.refresh_expires_in);
        refreshCookie.setAttribute("SameSite", "Strict");
        return refreshCookie;
    }

    @Nonnull
    private static Cookie getAuthFlowCookie(@Nonnull String state) {
        var cookie = new Cookie(AUTH_FLOW_COOKIE_NAME, state);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath(AUTH_FLOW_COOKIE_PATH);
        cookie.setMaxAge((int) AUTH_FLOW_TTL.toSeconds());
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    @Nonnull
    private static Cookie getExpiredAccessCookie() {
        return getExpiredCookie(ACCESS_COOKIE_NAME, ACCESS_COOKIE_PATH);
    }

    @Nonnull
    private static Cookie getExpiredRefreshCookie(@Nonnull String path) {
        return getExpiredCookie(REFRESH_COOKIE_NAME, path);
    }

    @Nonnull
    private static Cookie getExpiredAuthFlowCookie() {
        var cookie = getExpiredCookie(AUTH_FLOW_COOKIE_NAME, AUTH_FLOW_COOKIE_PATH);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    @Nonnull
    private static Cookie getExpiredCookie(@Nonnull String name, @Nonnull String path) {
        var cookie = new Cookie(name, "");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        return cookie;
    }

    @Nonnull
    private static String generateOpaqueValue() {
        return RandomUtils.generateRandomString(64);
    }

    @Nonnull
    static String createCodeChallenge(@Nonnull String codeVerifier) throws ResponseException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ResponseException.internalServerError(e, "Der SHA-256 Algorithmus wird nicht unterstützt.");
        }
        var hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private record AuthFlowState(
            @Nonnull
            String codeVerifier,
            @Nonnull
            String redirectUri,
            @Nonnull
            String appUri
    ) {
    }

    public record TokenResponse(
            @Nonnull
            String access_token,
            @Nonnull
            String refresh_token,
            @Nonnull
            Integer expires_in,
            @Nonnull
            Integer refresh_expires_in
    ) {
    }

    public record AuthStatusResponse(
            @Nonnull
            Long accessExpires,
            @Nonnull
            Long refreshExpires
    ) {
        public static AuthStatusResponse of(TokenResponse tokenResponse) {
            var now = Instant.now();
            return new AuthStatusResponse(
                    now.plusSeconds(tokenResponse.expires_in).toEpochMilli(),
                    now.plusSeconds(tokenResponse.refresh_expires_in).toEpochMilli()
            );
        }
    }
}
