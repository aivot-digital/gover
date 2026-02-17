package de.aivot.GoverBackend.identity.controllers;

import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.GoverBackend.identity.models.IdentityData;
import de.aivot.GoverBackend.identity.services.IdentityService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/identity/")
@Tag(
        name = "Identity",
        description = "These endpoints are used for authentication with external identity providers and retrieving identity data."
)
public class IdentityController {
    public static final String IDENTITY_HEADER_NAME = "gover-identity-id";

    private final IdentityCacheRepository identityCacheRepository;
    private final IdentityService identityService;

    @Autowired
    public IdentityController(IdentityCacheRepository identityCacheRepository,
                              IdentityService identityService) {
        this.identityCacheRepository = identityCacheRepository;
        this.identityService = identityService;
    }

    @GetMapping("{providerKey}/start/")
    @Operation(
            summary = "Start Identity Provider Authentication",
            description = "Initiates the authentication process with the specified identity provider."
    )
    public void start(
            @Nonnull @PathVariable UUID providerKey,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.ORIGIN, required = true) String origin,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.ADDITIONAL_SCOPES, required = false) List<String> additionalScopes,
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var redirectUrl = identityService
                .createRedirectURL(
                        providerKey,
                        origin,
                        additionalScopes
                );

        response
                .sendRedirect(redirectUrl.toString());
    }

    @GetMapping("{providerKey}/callback/{identitySessionId}/")
    @Operation(
            summary = "Handle Identity Provider Callback",
            description = "Processes the callback from the identity provider after authentication."
    )
    public void callback(
            @Nonnull @PathVariable UUID providerKey,
            @Nonnull @PathVariable UUID identitySessionId,
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
                        providerKey,
                        identitySessionId,
                        authorizationCode,
                        origin
                );

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("get/")
    @Operation(
            summary = "Get Identity Data",
            description = "Retrieves the identity data associated with the provided identity session ID."
    )
    public IdentityData get(
            @Nullable @RequestHeader(name = IDENTITY_HEADER_NAME, required = true) UUID identitySessionId
    ) throws ResponseException {
        if (identitySessionId == null) {
            throw ResponseException
                    .unauthorized("Sie haben sich bisher nicht angemeldet.");
        }

        var identityCacheEntity = identityCacheRepository
                .findById(identitySessionId)
                .orElseThrow(() -> ResponseException
                        .unauthorized("Sie haben sich bisher nicht angemeldet."));

        return IdentityData
                .from(identityCacheEntity);
    }
}
