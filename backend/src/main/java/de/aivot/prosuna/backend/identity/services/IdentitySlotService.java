package de.aivot.prosuna.backend.identity.services;

import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.identity.dtos.IdentityProviderOptionResponseDTO;
import de.aivot.prosuna.backend.identity.dtos.IdentitySlotResponseDTO;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves and mutates customer identity slots independently of the controller exposing them.
 */
@Service
public class IdentitySlotService {
    private final IdentityService identityService;
    private final IdentityProviderService identityProviderService;
    private final IdentityCommunicationService identityCommunicationService;
    private final CommunicationService communicationService;

    public IdentitySlotService(IdentityService identityService,
                               IdentityProviderService identityProviderService,
                               IdentityCommunicationService identityCommunicationService,
                               CommunicationService communicationService) {
        this.identityService = identityService;
        this.identityProviderService = identityProviderService;
        this.identityCommunicationService = identityCommunicationService;
        this.communicationService = communicationService;
    }

    @Nonnull
    public List<IdentitySlotResponseDTO> resolveSlots(@Nullable List<IdentityConfigElementSlot> configuredSlots,
                                                      @Nullable String identitySessionId,
                                                      @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        if (configuredSlots == null) {
            return new LinkedList<>();
        }

        var identities = identityService.getIdentityDataMap(identitySessionId, relatedProcessNodeId);
        var slots = new LinkedList<IdentitySlotResponseDTO>();
        for (var slot : configuredSlots) {
            if (slot != null) {
                slots.add(resolveSlot(slot, identities, identitySessionId, relatedProcessNodeId));
            }
        }
        return slots;
    }

    @Nonnull
    public IdentitySlotResponseDTO resolveSlot(@Nonnull IdentityConfigElementSlot slot,
                                               @Nullable String identitySessionId,
                                               @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        var identities = identityService.getIdentityDataMap(identitySessionId, relatedProcessNodeId);
        return resolveSlot(slot, identities, identitySessionId, relatedProcessNodeId);
    }

    @Nonnull
    public IdentitySlotResponseDTO resolveSlot(@Nonnull IdentityConfigElementSlot slot,
                                               @Nonnull IdentityDataMap identities,
                                               @Nullable String identitySessionId,
                                               @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        var identityId = requireConfiguredIdentityId(slot);
        var identityData = identities.get(identityId);
        var identityProviders = new ArrayList<IdentityProviderOptionResponseDTO>();

        for (var option : Optional.ofNullable(slot.getOptions()).orElse(List.of())) {
            if (option == null || option.getIdentityProviderKey() == null) {
                continue;
            }

            var identityProvider = identityProviderService.retrieve(option.getIdentityProviderKey()).orElse(null);
            if (identityProvider == null || !Boolean.TRUE.equals(identityProvider.getIsEnabled())) {
                continue;
            }
            if (communicationService.getUsableBindings(identityProvider).isEmpty()) {
                continue;
            }

            identityProviders.add(new IdentityProviderOptionResponseDTO(
                    identityProvider.getKey(),
                    identityProvider.getName(),
                    identityProvider.getIconAssetKey(),
                    identityProvider.getType(),
                    identityData != null
                            && identityData.type() == IdentityType.IdentityProvider
                            && Objects.equals(identityData.providerKey(), identityProvider.getKey()),
                    option.getAdditionalScopes() == null ? List.of() : option.getAdditionalScopes()
            ));
        }
        identityProviders.sort(Comparator.comparing(IdentityProviderOptionResponseDTO::identityProviderName));

        var selectedProviderIsConfigured = identityData != null
                && identityData.type() == IdentityType.IdentityProvider
                && identityProviders.stream().anyMatch(IdentityProviderOptionResponseDTO::isAuthenticatedWithThis);
        IdentityCommunicationService.SelectionState communicationSelection = null;
        if (selectedProviderIsConfigured && identitySessionId != null) {
            try {
                communicationSelection = identityCommunicationService.getState(
                        identitySessionId,
                        relatedProcessNodeId,
                        identityId
                );
            } catch (ResponseException ignored) {
                // Changed configuration makes the selection incomplete, not the task or form unretrievable.
            }
        }

        var emailIdentityIsAllowed = identityData != null
                && identityData.type() == IdentityType.Email
                && Boolean.TRUE.equals(slot.getAllowsMail());
        var ready = emailIdentityIsAllowed
                || (selectedProviderIsConfigured && communicationSelection != null && communicationSelection.ready());

        return new IdentitySlotResponseDTO(
                identityId,
                slot.getTitle(),
                slot.getDescription(),
                Boolean.TRUE.equals(slot.getIsOptional()),
                Boolean.TRUE.equals(slot.getAllowsMail()),
                identityData == null ? null : identityData.type(),
                identityData != null && identityData.type() == IdentityType.Email
                        ? identityData.emailAddress()
                        : null,
                ready,
                identityProviders,
                communicationSelection
        );
    }

    @Nonnull
    public URI createAuthenticationRedirect(@Nonnull IdentityConfigElementSlot slot,
                                            @Nonnull String requestedIdentityId,
                                            @Nonnull UUID providerKey,
                                            @Nullable String identitySessionId,
                                            @Nonnull String origin,
                                            @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        requireMatchingIdentityId(slot, requestedIdentityId);
        var provider = requireConfiguredProvider(slot, providerKey);
        return identityService.createRedirectURL(
                identitySessionId,
                providerKey,
                requestedIdentityId,
                origin,
                provider.option().getAdditionalScopes() == null
                        ? List.of()
                        : provider.option().getAdditionalScopes(),
                relatedProcessNodeId
        );
    }

    @Nonnull
    public IdentitySlotMutationResult setEmailIdentity(@Nonnull IdentityConfigElementSlot slot,
                                                        @Nonnull String requestedIdentityId,
                                                        @Nullable String identitySessionId,
                                                        @Nonnull Integer relatedProcessNodeId,
                                                        @Nullable String emailAddress) throws ResponseException {
        requireMatchingIdentityId(slot, requestedIdentityId);
        if (!Boolean.TRUE.equals(slot.getAllowsMail())) {
            throw ResponseException.badRequest("Die direkte E-Mail-Eingabe ist für diese Identität nicht erlaubt.");
        }

        final IdentityData identity;
        try {
            identity = IdentityData.from(identityService.setEmailIdentity(
                    identitySessionId,
                    relatedProcessNodeId,
                    requestedIdentityId,
                    emailAddress
            ));
        } catch (IllegalArgumentException e) {
            throw ResponseException.badRequest(e.getMessage());
        }

        var identities = identityService.getIdentityDataMap(identity.sessionId(), relatedProcessNodeId);
        return new IdentitySlotMutationResult(
                resolveSlot(slot, identities, identity.sessionId(), relatedProcessNodeId),
                identity.sessionId()
        );
    }

    public boolean clearIdentity(@Nonnull IdentityConfigElementSlot slot,
                                 @Nonnull String requestedIdentityId,
                                 @Nullable String identitySessionId,
                                 @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        requireMatchingIdentityId(slot, requestedIdentityId);
        return identityService.clearIdentity(identitySessionId, relatedProcessNodeId, requestedIdentityId);
    }

    @Nonnull
    public IdentityCommunicationService.SelectionState selectCommunication(
            @Nonnull IdentityConfigElementSlot slot,
            @Nonnull String requestedIdentityId,
            @Nonnull String identitySessionId,
            @Nonnull Integer relatedProcessNodeId,
            @Nonnull Integer bindingId,
            @Nonnull AuthoredElementValues customerData
    ) throws ResponseException {
        requireConfiguredCachedProvider(slot, requestedIdentityId, identitySessionId, relatedProcessNodeId);
        return identityCommunicationService.select(
                identitySessionId,
                relatedProcessNodeId,
                requestedIdentityId,
                bindingId,
                customerData
        );
    }

    @Nonnull
    public IdentityCommunicationService.SelectionState previewCommunication(
            @Nonnull IdentityConfigElementSlot slot,
            @Nonnull String requestedIdentityId,
            @Nonnull String identitySessionId,
            @Nonnull Integer relatedProcessNodeId,
            @Nonnull Integer bindingId,
            @Nonnull AuthoredElementValues customerData,
            @Nonnull List<String> skipErrorsForElementIds
    ) throws ResponseException {
        requireConfiguredCachedProvider(slot, requestedIdentityId, identitySessionId, relatedProcessNodeId);
        return identityCommunicationService.preview(
                identitySessionId,
                relatedProcessNodeId,
                requestedIdentityId,
                bindingId,
                customerData,
                skipErrorsForElementIds
        );
    }

    @Nonnull
    public String requireConfiguredIdentityId(@Nonnull IdentityConfigElementSlot slot) throws ResponseException {
        var identityId = slot.getId();
        if (identityId == null || identityId.isBlank()) {
            throw ResponseException.internalServerError(
                    "Für den konfigurierten Identitäts-Slot fehlt eine gültige ID."
            );
        }
        return identityId;
    }

    private void requireMatchingIdentityId(@Nonnull IdentityConfigElementSlot slot,
                                           @Nonnull String requestedIdentityId) throws ResponseException {
        if (!Objects.equals(requireConfiguredIdentityId(slot), requestedIdentityId)) {
            throw ResponseException.notFound("Die konfigurierte Identität wurde nicht gefunden.");
        }
    }

    @Nonnull
    private ConfiguredIdentityProvider requireConfiguredProvider(@Nonnull IdentityConfigElementSlot slot,
                                                                 @Nonnull UUID providerKey) throws ResponseException {
        var configuredOption = Optional.ofNullable(slot.getOptions())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(option -> Objects.equals(option.getIdentityProviderKey(), providerKey))
                .findFirst()
                .orElseThrow(() -> ResponseException.badRequest(
                        "Der Nutzerkontenanbieter ist für diese Identität nicht konfiguriert."
                ));
        var identityProvider = identityProviderService.retrieve(providerKey)
                .orElseThrow(() -> ResponseException.notFound("Der Nutzerkontenanbieter existiert nicht."));
        if (!Boolean.TRUE.equals(identityProvider.getIsEnabled())) {
            throw ResponseException.badRequest("Der Nutzerkontenanbieter ist nicht aktiviert.");
        }
        if (communicationService.getUsableBindings(identityProvider).isEmpty()) {
            throw ResponseException.conflict(
                    "Für den Nutzerkontenanbieter ist keine verwendbare Kommunikationsanbindung konfiguriert."
            );
        }
        return new ConfiguredIdentityProvider(configuredOption);
    }

    private void requireConfiguredCachedProvider(@Nonnull IdentityConfigElementSlot slot,
                                                 @Nonnull String requestedIdentityId,
                                                 @Nonnull String identitySessionId,
                                                 @Nonnull Integer relatedProcessNodeId) throws ResponseException {
        requireMatchingIdentityId(slot, requestedIdentityId);
        var identity = identityService
                .getIdentityDataMap(identitySessionId, relatedProcessNodeId)
                .get(requestedIdentityId);
        if (identity == null || identity.type() != IdentityType.IdentityProvider || identity.providerKey() == null) {
            throw ResponseException.notFound("Die authentifizierte Identität wurde nicht gefunden.");
        }
        requireConfiguredProvider(slot, identity.providerKey());
    }

    public record IdentitySlotMutationResult(
            @Nonnull IdentitySlotResponseDTO slot,
            @Nonnull String identitySessionId
    ) {
    }

    private record ConfiguredIdentityProvider(
            @Nonnull IdentityConfigElementOption option
    ) {
    }
}
