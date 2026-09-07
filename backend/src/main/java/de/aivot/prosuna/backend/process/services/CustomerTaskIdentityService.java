package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition.CustomerView;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CustomerTaskIdentityService {
    public static final String REQUIRED_IDENTITY_AUTHENTICATION_REASON = "required_identity_authentication";

    private final IdentityService identityService;

    public CustomerTaskIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public void requireAuthenticatedIdentity(@Nonnull ProcessInstanceEntity processInstance,
                                             @Nonnull ProcessNodeEntity processNode,
                                             @Nonnull CustomerView customerView,
                                             @Nullable String identitySessionId) throws ResponseException {
        var requiredIdentity = resolveRequiredProviderIdentity(processInstance, customerView);
        if (requiredIdentity == null) {
            return;
        }

        var authenticatedIdentities = identityService
                .getIdentityDataMap(identitySessionId, processNode.getId());
        var isRequiredIdentityAuthenticated = authenticatedIdentities
                .values()
                .stream()
                .map(IdentityData::uniqueIdFromIdentityProvider)
                .anyMatch(uniqueId -> Objects.equals(uniqueId, requiredIdentity.identity().uniqueIdFromIdentityProvider()));

        if (!isRequiredIdentityAuthenticated) {
            throw ResponseException.unauthorizedWithDetails(
                    "Für diese Aufgabe ist eine erneute Anmeldung mit dem zugehörigen Nutzerkonto erforderlich.",
                    Map.of("reason", REQUIRED_IDENTITY_AUTHENTICATION_REASON)
            );
        }
    }

    @Nonnull
    public URI createAuthenticationRedirect(@Nonnull ProcessInstanceEntity processInstance,
                                            @Nonnull ProcessNodeEntity processNode,
                                            @Nonnull CustomerView customerView,
                                            @Nullable String identitySessionId,
                                            @Nonnull String origin) throws ResponseException {
        var requiredIdentity = resolveRequiredProviderIdentity(processInstance, customerView);
        if (requiredIdentity == null) {
            throw ResponseException.badRequest("Für diese Aufgabe ist keine Anmeldung mit einem Nutzerkonto erforderlich.");
        }

        return identityService.createRedirectURL(
                identitySessionId,
                requiredIdentity.identity().providerKey(),
                requiredIdentity.identityId(),
                origin,
                List.of(),
                processNode.getId()
        );
    }

    @Nullable
    private RequiredIdentity resolveRequiredProviderIdentity(@Nonnull ProcessInstanceEntity processInstance,
                                                             @Nonnull CustomerView customerView) throws ResponseException {
        var requiredIdentityId = normalizeRequiredIdentityId(customerView.requiredIdentityId());
        if (requiredIdentityId == null) {
            return null;
        }

        var requiredIdentity = processInstance
                .getIdentities()
                .get(requiredIdentityId);
        if (requiredIdentity == null) {
            throw ResponseException.internalServerError(
                    "Die für die Kundenaufgabe erforderliche Identität ist in der Prozessinstanz nicht vorhanden."
            );
        }

        if (requiredIdentity.type() == IdentityType.Email) {
            return null;
        }

        if (requiredIdentity.providerKey() == null
                || requiredIdentity.uniqueIdFromIdentityProvider() == null
                || requiredIdentity.uniqueIdFromIdentityProvider().isBlank()) {
            throw ResponseException.internalServerError(
                    "Die für die Kundenaufgabe erforderliche Anbieteridentität ist unvollständig."
            );
        }

        return new RequiredIdentity(requiredIdentityId, requiredIdentity);
    }

    @Nullable
    private String normalizeRequiredIdentityId(@Nullable String requiredIdentityId) {
        if (requiredIdentityId == null || requiredIdentityId.isBlank()) {
            return null;
        }
        return requiredIdentityId.trim();
    }

    private record RequiredIdentity(
            @Nonnull String identityId,
            @Nonnull IdentityData identity
    ) {
    }
}
