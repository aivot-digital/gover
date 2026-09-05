package de.aivot.prosuna.backend.identity.controllers;

import de.aivot.prosuna.backend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.utils.IdentityCookieUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/identity/")
@Tag(
        name = "Identity",
        description = "These endpoints are used for authentication with external identity providers and retrieving identity data."
)
public class IdentityController {
    public static final String IDENTITY_COOKIE_NAME = IdentityCookieUtils.IDENTITY_COOKIE_NAME;
    public static final String IDENTITY_COOKIE_PATH = IdentityCookieUtils.IDENTITY_COOKIE_PATH;

    private final IdentityService identityService;
    private final IdentityCommunicationService identityCommunicationService;

    @Autowired
    public IdentityController(IdentityService identityService,
                              IdentityCommunicationService identityCommunicationService) {
        this.identityService = identityService;
        this.identityCommunicationService = identityCommunicationService;
    }

    @GetMapping("{providerKey}/callback/{identitySessionId}/{identityCacheEntityId}/")
    @Operation(
            summary = "Handle Identity Provider Callback",
            description = "Processes the callback from the identity provider after authentication."
    )
    public void callback(
            @Nonnull @PathVariable UUID providerKey,
            @Nonnull @PathVariable String identitySessionId,
            @Nonnull @PathVariable String identityCacheEntityId,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_STATE) String state,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_ERROR, required = false) String error,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_ERROR_DESCRIPTION, required = false) String errorDescription,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.REMOTE_AUTH_AUTHORIZATION_CODE, required = false) String authorizationCode,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        if (error != null) {
            var redirectUrl = identityService
                    .createErrorRedirectURL(
                            identityCacheEntityId,
                            identitySessionId,
                            state,
                            error,
                            errorDescription
                    );
            response.sendRedirect(redirectUrl);
            return;
        }

        var redirectUrl = identityService
                .handleCallback(
                        providerKey,
                        identityCacheEntityId,
                        identitySessionId,
                        authorizationCode,
                        state
                );

        response.addCookie(IdentityCookieUtils.createIdentityCookie(identitySessionId));
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("get/")
    @Operation(
            summary = "Get Identity Data",
            description = "Retrieves the identity data associated with the provided identity session ID."
    )
    public IdentityDataMap get(
            @Nonnull @CookieValue(name = IDENTITY_COOKIE_NAME, required = true) String identitySessionId,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.CLEAR, required = false) Boolean clear,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID, required = false) Integer relatedProcessNodeId,
            @Nonnull HttpServletResponse response
    ) throws ResponseException {
        try {
            return identityService
                    .getIdentityDataMap(identitySessionId, relatedProcessNodeId);
        } finally {
            if (Boolean.TRUE.equals(clear)) {
                boolean someIdentitiesStillExist = identityService.clearIdentitySession(identitySessionId, relatedProcessNodeId);

                if (!someIdentitiesStillExist) {
                    response.addCookie(IdentityCookieUtils.createExpiredIdentityCookie());
                }
            }
        }
    }

    @DeleteMapping("session/")
    @Operation(
            summary = "Clear Identity Session",
            description = "Removes the current identity session from the backend cache and expires the local identity session cookie."
    )
    public void clearSession(
            @Nullable @CookieValue(name = IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nullable @RequestParam(name = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID, required = false) Integer relatedProcessNodeId,
            @Nonnull HttpServletResponse response
    ) {
        boolean someIdentitiesStillExist = true;
        try {
            someIdentitiesStillExist = identityService.clearIdentitySession(identitySessionId, relatedProcessNodeId);
        } finally {
            if (!someIdentitiesStillExist) {
                response.addCookie(IdentityCookieUtils.createExpiredIdentityCookie());
            }
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @GetMapping("{identityId}/communication/")
    @Operation(summary = "Get communication-provider selection for an authenticated identity")
    public IdentityCommunicationService.SelectionState getCommunicationSelection(
            @Nonnull @PathVariable String identityId,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID) Integer relatedProcessNodeId,
            @Nonnull @CookieValue(name = IDENTITY_COOKIE_NAME) String identitySessionId
    ) throws ResponseException {
        return identityCommunicationService.getState(identitySessionId, relatedProcessNodeId, identityId);
    }

    @PutMapping("{identityId}/communication/")
    @Operation(summary = "Select and configure a communication provider for an authenticated identity")
    public IdentityCommunicationService.SelectionState selectCommunicationProvider(
            @Nonnull @PathVariable String identityId,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID) Integer relatedProcessNodeId,
            @Nonnull @CookieValue(name = IDENTITY_COOKIE_NAME) String identitySessionId,
            @Nonnull @Valid @RequestBody CommunicationSelectionRequest request
    ) throws ResponseException {
        return identityCommunicationService.select(
                identitySessionId,
                relatedProcessNodeId,
                identityId,
                request.bindingId(),
                request.customerData()
        );
    }

    @PostMapping("{identityId}/communication/derive/")
    @Operation(summary = "Preview communication-provider customer configuration without saving it")
    public IdentityCommunicationService.SelectionState deriveCommunicationProviderConfiguration(
            @Nonnull @PathVariable String identityId,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID) Integer relatedProcessNodeId,
            @Nonnull @CookieValue(name = IDENTITY_COOKIE_NAME) String identitySessionId,
            @Nonnull @Valid @RequestBody CommunicationSelectionRequest request
    ) throws ResponseException {
        return identityCommunicationService.preview(
                identitySessionId,
                relatedProcessNodeId,
                identityId,
                request.bindingId(),
                request.customerData()
        );
    }

    public record CommunicationSelectionRequest(
            @Nonnull @NotNull Integer bindingId,
            @Nonnull @NotNull AuthoredElementValues customerData
    ) {
    }
}
