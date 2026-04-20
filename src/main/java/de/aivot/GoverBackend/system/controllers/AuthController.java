package de.aivot.GoverBackend.system.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {
    private static final String AUTH_PATH = "/protocol/openid-connect/auth";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";

    private static final String APP_URI_QUERY_PARAM = "app_uri";
    private static final String APP_ACCESS_EXPIRES_IN_PARAM = "access_expires_in";

    private static final String OIDC_CLIENT_ID_PARAM_KEY = "client_id";
    private static final String OIDC_CLIENT_SECRET_PARAM_KEY = "client_secret";
    private static final String OIDC_RESPONSE_TYPE_PARAM_KEY = "response_type";
    private static final String OIDC_RESPONSE_TYPE_VALUE = "code";
    private static final String OIDC_RESPONSE_SCOPE_PARAM_KEY = "scope";
    private static final String OIDC_RESPONSE_SCOPE_PARAM_VALUE = "openid profile email";
    private static final String OIDC_REDIRECT_URI_PARAM_KEY = "redirect_uri";

    private static final String OIDC_GRANT_TYPE_PARAM_KEY = "grant_type";
    private static final String OIDC_GRANT_TYPE_VALUE = "authorization_code";
    private static final String OIDC_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String OIDC_REFRESH_TOKEN_PARAM_KEY = "refresh_token";

    private static final String OIDC_CALLBACK_SESSION_STATE_PARAM_KEY = "session_state";
    private static final String OIDC_CALLBACK_ISS_PARAM_KEY = "iss";
    private static final String OIDC_CALLBACK_CODE_PARAM_KEY = "code";

    public static final String ACCESS_COOKIE_NAME = "access";
    public static final String REFRESH_COOKIE_NAME = "refresh";

    @Value("${gover.goverHostname}")
    private String hostname;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String oidcIssuerURI;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String oidcClientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String oidcClientSecret;

    private final HttpService httpService;

    public AuthController(HttpService httpService) {
        this.httpService = httpService;
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
        var callbackRedirectUri = UriComponentsBuilder
                .fromUriString(hostname)
                .path(request.getServletPath().replace("/login", "/oidc-callback"))
                .queryParam(APP_URI_QUERY_PARAM, appUri)
                .toUriString();

        var uriBuilder = UriComponentsBuilder
                .fromUriString(oidcIssuerURI)
                .path(AUTH_PATH)
                .queryParam(OIDC_CLIENT_ID_PARAM_KEY, oidcClientId)
                .queryParam(OIDC_REDIRECT_URI_PARAM_KEY, callbackRedirectUri)
                .queryParam(OIDC_RESPONSE_TYPE_PARAM_KEY, OIDC_RESPONSE_TYPE_VALUE)
                .queryParam(OIDC_RESPONSE_SCOPE_PARAM_KEY, OIDC_RESPONSE_SCOPE_PARAM_VALUE);

        if (StringUtils.isNotNullOrEmpty(oidcClientSecret)) {
            uriBuilder = uriBuilder.queryParam(OIDC_CLIENT_SECRET_PARAM_KEY, oidcClientSecret);
        }

        response.sendRedirect(uriBuilder.toUriString());
    }

    @GetMapping("refresh")
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public void refresh(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull @CookieValue(value = REFRESH_COOKIE_NAME) String refreshToken
    ) throws ResponseException {
        var payload = Map.of(
                OIDC_GRANT_TYPE_PARAM_KEY, OIDC_GRANT_TYPE_REFRESH_TOKEN,
                OIDC_CLIENT_ID_PARAM_KEY, oidcClientId,
                OIDC_REFRESH_TOKEN_PARAM_KEY, refreshToken
        );

        var tokenResponse = getTokenResponse(payload);

        response
                .addCookie(getRefreshCookie(tokenResponse));
        response
                .addCookie(getAccessCookie(tokenResponse));

        response.setStatus(HttpStatus.OK.value());
    }


    @GetMapping("oidc-callback")
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public void idpCallback(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull @RequestParam(value = APP_URI_QUERY_PARAM) String appUri,
            @Nonnull @RequestParam(value = OIDC_CALLBACK_SESSION_STATE_PARAM_KEY) String sessionState,
            @Nonnull @RequestParam(value = OIDC_CALLBACK_ISS_PARAM_KEY) String iss,
            @Nonnull @RequestParam(value = OIDC_CALLBACK_CODE_PARAM_KEY) String code
    ) throws ResponseException, IOException {
        var redirectTo = UriComponentsBuilder
                .fromUriString(hostname)
                .path(request.getServletPath())
                .queryParam(APP_URI_QUERY_PARAM, appUri)
                .toUriString();

        var payload = Map.of(
                OIDC_GRANT_TYPE_PARAM_KEY, OIDC_GRANT_TYPE_VALUE,
                OIDC_CLIENT_ID_PARAM_KEY, oidcClientId,
                OIDC_CALLBACK_CODE_PARAM_KEY, code,
                OIDC_REDIRECT_URI_PARAM_KEY, redirectTo,
                OIDC_RESPONSE_SCOPE_PARAM_KEY, OIDC_RESPONSE_SCOPE_PARAM_VALUE
        );

        TokenResponse tokenResponse = getTokenResponse(payload);

        var refreshCookie = getRefreshCookie(tokenResponse);
        response.addCookie(refreshCookie);

        var accessCookie = getAccessCookie(tokenResponse);
        response.addCookie(accessCookie);

        var appRedirectLocation = UriComponentsBuilder
                .fromUriString(appUri)
                .queryParam(APP_ACCESS_EXPIRES_IN_PARAM, tokenResponse.expires_in)
                .toUriString();

        response.sendRedirect(appRedirectLocation);
    }

    @Nonnull
    private TokenResponse getTokenResponse(@Nonnull Map<String, String> payload) throws ResponseException {
        var tokenUri = UriComponentsBuilder
                .fromUriString(oidcIssuerURI)
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
            throw ResponseException.internalServerError("Failed to exchange authorization code for access token");
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

    @Nonnull
    private static Cookie getAccessCookie(TokenResponse tokenResponse) {
        var accessCookie = new Cookie(ACCESS_COOKIE_NAME, tokenResponse.access_token);
        accessCookie.setSecure(true);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/api/");
        accessCookie.setMaxAge(tokenResponse.expires_in);
        return accessCookie;
    }

    @Nonnull
    private static Cookie getRefreshCookie(TokenResponse tokenResponse) {
        var refreshCookie = new Cookie(REFRESH_COOKIE_NAME, tokenResponse.refresh_token);
        refreshCookie.setSecure(true);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(tokenResponse.refresh_expires_in);
        return refreshCookie;
    }

    private record TokenResponse(
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
}
