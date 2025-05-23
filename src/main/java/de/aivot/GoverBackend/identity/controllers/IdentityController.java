package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.GoverBackend.identity.enums.IdentityResultState;
import de.aivot.GoverBackend.identity.models.IdentityData;
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

@RestController
@RequestMapping("/api/public/identity/")
public class IdentityController {
    public static final String IDENTITY_HEADER_NAME = "gover-identity-id";

    private final GoverConfig goverConfig;
    private final IdentityCacheRepository identityCacheRepository;
    private final IdentityService identityService;

    @Autowired
    public IdentityController(
            GoverConfig goverConfig,
            IdentityCacheRepository identityCacheRepository,
            IdentityService identityService
    ) {
        this.goverConfig = goverConfig;
        this.identityCacheRepository = identityCacheRepository;
        this.identityService = identityService;
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
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_STATE) String origin,
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

        var redirectUrl = identityService
                .handleCallback(
                        key,
                        authorizationCode,
                        createCallbackURI(key),
                        origin
                );

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("get/")
    public IdentityData get(
            @Nullable @RequestHeader(name = IDENTITY_HEADER_NAME, required = true) String identityId
    ) throws ResponseException {
        if (identityId == null) {
            throw ResponseException
                    .unauthorized("Sie haben sich bisher nicht angemeldet.");
        }

        var identityCacheEntity = identityCacheRepository
                .findById(identityId)
                .orElseThrow(() -> ResponseException
                        .unauthorized("Sie haben sich bisher nicht angemeldet."));

        return IdentityData
                .from(identityCacheEntity);
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
