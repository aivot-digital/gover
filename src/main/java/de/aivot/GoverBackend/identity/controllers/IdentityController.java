package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.GoverBackend.identity.dtos.IdentityDetailsDTO;
import de.aivot.GoverBackend.identity.enums.IdentityResultState;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.identity.services.IdentityService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/identity/")
public class IdentityController {
    private static final String IDENTITY_COOKIE_NAME = "identity";

    private final IdentityProviderRepository identityProviderRepository;
    private final GoverConfig goverConfig;
    private final IdentityCacheRepository identityCacheRepository;
    private final IdentityService identityService;

    @Autowired
    public IdentityController(
            IdentityProviderRepository identityProviderRepository,
            GoverConfig goverConfig,
            IdentityCacheRepository identityCacheRepository,
            IdentityService identityService
    ) {
        this.identityProviderRepository = identityProviderRepository;
        this.goverConfig = goverConfig;
        this.identityCacheRepository = identityCacheRepository;
        this.identityService = identityService;
    }

    @GetMapping("{key}/info/")
    public IdentityDetailsDTO info(
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        var provider = identityProviderRepository
                .findById(key)
                .orElseThrow(ResponseException::notFound);

        return IdentityDetailsDTO
                .from(provider);
    }

    @GetMapping("{key}/start/")
    public void start(
            @Nonnull @PathVariable String key,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.ADDITIONAL_SCOPES, required = false) List<String> additionalScopes,
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var redirectUrl = identityService
                .createRedirectURL(
                        key,
                        createCallbackURI(key),
                        request.getHeader(HttpHeaders.REFERER),
                        additionalScopes
                );

        response
                .sendRedirect(redirectUrl.toString());
    }

    @GetMapping("{key}/callback/")
    public void callback(
            @Nonnull @PathVariable String key,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.ORIGIN) String origin,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_ERROR, required = false) String error,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_ERROR_DESCRIPTION, required = false) String errorDescription,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_AUTHORIZATION_CODE, required = false) String authorizationCode,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        if (error != null) {
            var redirectUrl = identityService
                    .createErrorRedirectURL(
                            origin,
                            error,
                            errorDescription
                    );
            response.sendRedirect(redirectUrl);
            return;
        }

        var identityData = identityService
                .handleCallback(
                        key,
                        authorizationCode,
                        createCallbackURI(key),
                        origin
                );

        var identityCookie = new Cookie(IDENTITY_COOKIE_NAME, identityData.getId());
        identityCookie.setPath("/");
        response.addCookie(identityCookie);

        var redirectUrl = UriComponentsBuilder
                .fromUriString(origin)
                .queryParam(IdentityQueryParameterConstants.RESULT_STATE_CODE, IdentityResultState.Success.getKey())
                .build()
                .toString();

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("get/")
    public Map<String, String> get(
            @Nullable @CookieValue(IDENTITY_COOKIE_NAME) String identityId
    ) throws ResponseException {
        if (identityId == null) {
            throw ResponseException
                    .unauthorized("Sie haben sich bisher nicht angemeldet.");
        }

        var identityCacheEntity = identityCacheRepository
                .findById(identityId)
                .orElseThrow(() -> ResponseException
                        .unauthorized("Sie haben sich bisher nicht angemeldet."));

        return identityCacheEntity
                .getIdentityData();
    }

    // region Utility methods

    @Nonnull
    private URI createCallbackURI(
            @Nonnull String key
    ) {
        var callbackUrl = goverConfig
                .createUrl("/api/public/identity/" + key + "/callback/");
        return URI
                .create(callbackUrl);
    }

    // endregion
}
