package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.identity.dtos.IdentityProviderOptionResponseDTO;
import de.aivot.prosuna.backend.identity.dtos.IdentitySlotResponseDTO;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.identity.services.IdentitySlotService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeCustomerView;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
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
    private final IdentityProviderService identityProviderService;
    private final IdentitySlotService identitySlotService;

    public CustomerTaskIdentityService(IdentityService identityService,
                                       IdentityProviderService identityProviderService,
                                       IdentitySlotService identitySlotService) {
        this.identityService = identityService;
        this.identityProviderService = identityProviderService;
        this.identitySlotService = identitySlotService;
    }

    @Nonnull
    public CustomerTaskIdentityState resolveIdentityState(@Nonnull ProcessInstanceEntity processInstance,
                                                          @Nonnull ProcessNodeEntity processNode,
                                                          @Nonnull ProcessNodeCustomerView processNodeCustomerView,
                                                          @Nullable String identitySessionId) throws ResponseException {
        var requiredExistingIdentity = resolveRequiredProviderIdentity(processInstance, processNodeCustomerView);
        var requiredNewIdentitySlot = resolveRequiredNewIdentitySlot(processInstance, processNodeCustomerView);
        if (requiredExistingIdentity == null && requiredNewIdentitySlot == null) {
            return CustomerTaskIdentityState.readyWithoutRequirements();
        }

        var cachedIdentities = identityService.getIdentityDataMap(identitySessionId, processNode.getId());
        ExistingIdentityState existingIdentityState = null;
        if (requiredExistingIdentity != null) {
            var isAuthenticatedWithProvider = cachedIdentities
                    .values()
                    .stream()
                    .anyMatch(identity -> isSameProvider(identity, requiredExistingIdentity.identity()));
            var isReady = cachedIdentities
                    .values()
                    .stream()
                    .anyMatch(identity -> isSameProviderAccount(identity, requiredExistingIdentity.identity()));
            var provider = requiredExistingIdentity.provider();
            existingIdentityState = new ExistingIdentityState(
                    requiredExistingIdentity.identityId(),
                    isReady,
                    new IdentityProviderOptionResponseDTO(
                            provider.getKey(),
                            provider.getName(),
                            provider.getIconAssetKey(),
                            provider.getType(),
                            isAuthenticatedWithProvider,
                            List.of()
                    )
            );
        }

        IdentitySlotResponseDTO newIdentitySlot = null;
        IdentityData newIdentity = null;
        if (requiredNewIdentitySlot != null) {
            newIdentitySlot = identitySlotService.resolveSlot(
                    requiredNewIdentitySlot,
                    cachedIdentities,
                    identitySessionId,
                    processNode.getId()
            );
            if (newIdentitySlot.isReady()) {
                newIdentity = cachedIdentities.get(newIdentitySlot.id());
            }
        }

        var existingIdentityIsReady = existingIdentityState == null || existingIdentityState.isReady();
        var newIdentityIsReady = newIdentitySlot == null
                || newIdentitySlot.isReady()
                || (newIdentitySlot.isOptional() && newIdentitySlot.identityType() == null);

        return new CustomerTaskIdentityState(
                existingIdentityState,
                newIdentitySlot,
                newIdentity,
                existingIdentityIsReady && newIdentityIsReady
        );
    }

    @Nonnull
    public CustomerTaskIdentityState requireAuthenticatedIdentity(@Nonnull ProcessInstanceEntity processInstance,
                                                                  @Nonnull ProcessNodeEntity processNode,
                                                                  @Nonnull ProcessNodeCustomerView processNodeCustomerView,
                                                                  @Nullable String identitySessionId) throws ResponseException {
        var state = resolveIdentityState(processInstance, processNode, processNodeCustomerView, identitySessionId);
        requireAuthenticatedIdentity(state);
        return state;
    }

    public void requireAuthenticatedIdentity(@Nonnull CustomerTaskIdentityState state) throws ResponseException {
        if (!state.isReady()) {
            throw ResponseException.unauthorizedWithDetails(
                    "Für diese Aufgabe müssen zunächst alle erforderlichen Identitäten vollständig angegeben werden.",
                    Map.of("reason", REQUIRED_IDENTITY_AUTHENTICATION_REASON)
            );
        }
    }

    @Nonnull
    public URI createAuthenticationRedirect(@Nonnull ProcessInstanceEntity processInstance,
                                            @Nonnull ProcessNodeEntity processNode,
                                            @Nonnull ProcessNodeCustomerView processNodeCustomerView,
                                            @Nullable String identitySessionId,
                                            @Nonnull String origin) throws ResponseException {
        var requiredIdentity = resolveRequiredProviderIdentity(processInstance, processNodeCustomerView);
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

    @Nonnull
    public IdentityConfigElementSlot requireNewIdentitySlot(@Nonnull ProcessInstanceEntity processInstance,
                                                            @Nonnull ProcessNodeCustomerView processNodeCustomerView,
                                                            @Nonnull String requestedIdentityId) throws ResponseException {
        var slot = resolveRequiredNewIdentitySlot(processInstance, processNodeCustomerView);
        if (slot == null || !Objects.equals(slot.getId(), requestedIdentityId)) {
            throw ResponseException.notFound("Die konfigurierte Identität wurde nicht gefunden.");
        }
        return slot;
    }

    @Nonnull
    public Map<String, IdentityData> getAdditionalIdentitiesForCompletion(
            @Nonnull CustomerTaskIdentityState state,
            @Nonnull ProcessNodeExecutionResult executionResult
    ) {
        if (!isCompletingResult(executionResult) || state.newIdentity() == null) {
            return Map.of();
        }
        return Map.of(state.newIdentity().identityId(), state.newIdentity());
    }

    public boolean isCompletingResult(@Nonnull ProcessNodeExecutionResult executionResult) {
        return executionResult instanceof ProcessNodeExecutionResultTaskCompleted
                || executionResult instanceof ProcessNodeExecutionResultInstanceCompleted;
    }

    public boolean clearTaskIdentitySession(@Nullable String identitySessionId,
                                            @Nonnull ProcessNodeEntity processNode) {
        return identityService.clearIdentitySession(identitySessionId, processNode.getId());
    }

    @Nullable
    private RequiredIdentity resolveRequiredProviderIdentity(@Nonnull ProcessInstanceEntity processInstance,
                                                             @Nonnull ProcessNodeCustomerView processNodeCustomerView) throws ResponseException {
        var requiredIdentityId = normalizeRequiredIdentityId(processNodeCustomerView.requiredExistingIdentityId());
        if (requiredIdentityId == null) {
            return null;
        }

        var processIdentities = processInstance.getIdentities();
        var requiredIdentity = processIdentities == null ? null : processIdentities.get(requiredIdentityId);
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

        var provider = identityProviderService
                .retrieve(requiredIdentity.providerKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Nutzerkontenanbieter der erforderlichen Identität ist nicht vorhanden."
                ));
        if (!Boolean.TRUE.equals(provider.getIsEnabled())) {
            throw ResponseException.internalServerError(
                    "Der Nutzerkontenanbieter der erforderlichen Identität ist nicht aktiviert."
            );
        }

        return new RequiredIdentity(requiredIdentityId, requiredIdentity, provider);
    }

    @Nullable
    private IdentityConfigElementSlot resolveRequiredNewIdentitySlot(
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessNodeCustomerView processNodeCustomerView
    ) throws ResponseException {
        var slot = processNodeCustomerView.requiredNewIdentitySlot();
        if (slot == null) {
            return null;
        }

        var identityId = identitySlotService.requireConfiguredIdentityId(slot);
        var processIdentities = processInstance.getIdentities();
        if (processIdentities != null && processIdentities.containsKey(identityId)) {
            throw ResponseException.internalServerError(
                    "Die ID der neu anzulegenden Identität wird bereits von einer Identität der Prozessinstanz verwendet."
            );
        }
        return slot;
    }

    private boolean isSameProvider(@Nonnull IdentityData candidate, @Nonnull IdentityData requiredIdentity) {
        return candidate.type() == IdentityType.IdentityProvider
                && Objects.equals(candidate.providerKey(), requiredIdentity.providerKey());
    }

    private boolean isSameProviderAccount(@Nonnull IdentityData candidate, @Nonnull IdentityData requiredIdentity) {
        return isSameProvider(candidate, requiredIdentity)
                && Objects.equals(
                candidate.uniqueIdFromIdentityProvider(),
                requiredIdentity.uniqueIdFromIdentityProvider()
        );
    }

    @Nullable
    private String normalizeRequiredIdentityId(@Nullable String requiredIdentityId) {
        if (requiredIdentityId == null || requiredIdentityId.isBlank()) {
            return null;
        }
        return requiredIdentityId.trim();
    }

    public record CustomerTaskIdentityState(
            @Nullable ExistingIdentityState existingIdentity,
            @Nullable IdentitySlotResponseDTO newIdentitySlot,
            @Nullable IdentityData newIdentity,
            boolean isReady
    ) {
        @Nonnull
        public static CustomerTaskIdentityState readyWithoutRequirements() {
            return new CustomerTaskIdentityState(null, null, null, true);
        }
    }

    public record ExistingIdentityState(
            @Nonnull String id,
            boolean isReady,
            @Nonnull IdentityProviderOptionResponseDTO identityProvider
    ) {
    }

    private record RequiredIdentity(
            @Nonnull String identityId,
            @Nonnull IdentityData identity,
            @Nonnull IdentityProviderEntity provider
    ) {
    }
}
