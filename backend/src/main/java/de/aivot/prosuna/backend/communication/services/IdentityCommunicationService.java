package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.cache.entities.IdentityCacheEntity;
import de.aivot.prosuna.backend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** Coordinates provider selection and provider-specific customer input for cached identities. */
@Service
public class IdentityCommunicationService {
    private final IdentityCacheRepository identityCacheRepository;
    private final CommunicationService communicationService;

    public IdentityCommunicationService(IdentityCacheRepository identityCacheRepository,
                                        CommunicationService communicationService) {
        this.identityCacheRepository = identityCacheRepository;
        this.communicationService = communicationService;
    }

    @Nonnull
    public SelectionState getState(@Nonnull String identitySessionId,
                                   @Nonnull Integer relatedProcessNodeId,
                                   @Nonnull String identityId) throws ResponseException {
        var cacheEntity = getAuthenticatedIdentity(identitySessionId, relatedProcessNodeId, identityId);
        return normalizeAndCreateState(cacheEntity);
    }

    @Nonnull
    public SelectionState select(@Nonnull String identitySessionId,
                                 @Nonnull Integer relatedProcessNodeId,
                                 @Nonnull String identityId,
                                 @Nonnull Integer bindingId,
                                 @Nonnull AuthoredElementValues customerData) throws ResponseException {
        var cacheEntity = getAuthenticatedIdentity(identitySessionId, relatedProcessNodeId, identityId);
        var identity = IdentityData.from(cacheEntity);
        List<CommunicationProviderBindingEntity> available;
        try {
            available = communicationService.getAvailableBindings(identity);
        } catch (CommunicationException e) {
            throw ResponseException.internalServerError("Fehler beim Abrufen der verfügbaren Kommunikationsanbieter.", e);
        }
        requireAvailableBinding(available);
        if (available.stream().noneMatch(binding -> Objects.equals(binding.getId(), bindingId))) {
            throw ResponseException.badRequest("Der ausgewählte Kommunikationsanbieter steht für diese Identität nicht zur Verfügung.");
        }

        cacheEntity.setCommunicationProviderBindingId(bindingId);
        var state = createState(IdentityData.from(cacheEntity), available, new ElementDerivationOptions(), customerData);
        // Store resolved values in identity snapshots, not the editor's input-mode envelopes.
        cacheEntity.setCommunicationProviderData(new HashMap<>(state.derivedData().getEffectiveValues()));
        identityCacheRepository.save(cacheEntity);
        return state;
    }

    /**
     * Derives the customer configuration for a prospective selection without changing the cached identity.
     */
    @Nonnull
    public SelectionState preview(@Nonnull String identitySessionId,
                                  @Nonnull Integer relatedProcessNodeId,
                                  @Nonnull String identityId,
                                  @Nonnull Integer bindingId,
                                  @Nonnull AuthoredElementValues customerData,
                                  @Nonnull List<String> skipErrorsForElementIds) throws ResponseException {
        var cacheEntity = getAuthenticatedIdentity(identitySessionId, relatedProcessNodeId, identityId);
        var identity = IdentityData.from(cacheEntity);
        List<CommunicationProviderBindingEntity> available;
        try {
            available = communicationService.getAvailableBindings(identity);
        } catch (CommunicationException e) {
            throw ResponseException.internalServerError("Fehler beim Abrufen der verfügbaren Kommunikationsanbieter.", e);
        }
        requireAvailableBinding(available);
        if (available.stream().noneMatch(binding -> Objects.equals(binding.getId(), bindingId))) {
            throw ResponseException.badRequest("Der ausgewählte Kommunikationsanbieter steht für diese Identität nicht zur Verfügung.");
        }

        var previewIdentity = new IdentityData(
                identity.sessionId(),
                identity.identityId(),
                identity.type(),
                identity.providerKey(),
                identity.metadataIdentifier(),
                identity.uniqueIdFromIdentityProvider(),
                identity.emailAddress(),
                identity.attributes(),
                bindingId,
                identity.communicationProviderData()
        );
        return createState(
                previewIdentity,
                available,
                new ElementDerivationOptions().setSkipErrorsForElementIds(skipErrorsForElementIds),
                customerData
        );
    }

    @Nonnull
    private SelectionState normalizeAndCreateState(@Nonnull IdentityCacheEntity cacheEntity) throws ResponseException {
        var identity = IdentityData.from(cacheEntity);
        List<CommunicationProviderBindingEntity> available;
        try {
            available = communicationService.getAvailableBindings(identity);
        } catch (CommunicationException e) {
            throw ResponseException.internalServerError("Fehler beim Abrufen der verfügbaren Kommunikationsanbieter.", e);
        }
        requireAvailableBinding(available);
        var selectedIsAvailable = available.stream()
                .anyMatch(binding -> Objects.equals(binding.getId(), cacheEntity.getCommunicationProviderBindingId()));

        Integer normalizedSelection = selectedIsAvailable ? cacheEntity.getCommunicationProviderBindingId() : null;
        if (normalizedSelection == null && available.size() == 1) {
            normalizedSelection = available.getFirst().getId();
        }

        if (!Objects.equals(normalizedSelection, cacheEntity.getCommunicationProviderBindingId())) {
            cacheEntity.setCommunicationProviderBindingId(normalizedSelection);
            cacheEntity.setCommunicationProviderData(new HashMap<>());
            identityCacheRepository.save(cacheEntity);
        }
        return createState(
                IdentityData.from(cacheEntity),
                available,
                new ElementDerivationOptions().setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS))
        );
    }

    @Nonnull
    private SelectionState createState(@Nonnull IdentityData identity,
                                       @Nonnull List<CommunicationProviderBindingEntity> available,
                                       @Nonnull ElementDerivationOptions derivationOptions) throws ResponseException {
        return createState(identity, available, derivationOptions, null);
    }

    @Nonnull
    private SelectionState createState(@Nonnull IdentityData identity,
                                       @Nonnull List<CommunicationProviderBindingEntity> available,
                                       @Nonnull ElementDerivationOptions derivationOptions,
                                       @Nullable AuthoredElementValues editedValues) throws ResponseException {
        var selectedBindingId = identity.communicationProviderBindingId();
        var choices = available.stream().map(BindingChoice::from).toList();

        if (selectedBindingId == null) {
            return new SelectionState(true, false, null, choices, null, new AuthoredElementValues(), DerivedRuntimeElementData.empty());
        }

        CommunicationService.CustomerConfiguration customerConfiguration = null;
        try {
            customerConfiguration = editedValues == null
                    ? communicationService.getCustomerConfiguration(identity, derivationOptions)
                    : communicationService.getCustomerConfiguration(identity, derivationOptions, editedValues);
        } catch (CommunicationException e) {
            throw ResponseException.internalServerError("Fehler beim Abrufen der Konfiguration für den ausgewählten Kommunikationsanbieter.", e);
        }
        return new SelectionState(
                true,
                customerConfiguration.ready(),
                selectedBindingId,
                choices,
                customerConfiguration.layout(),
                customerConfiguration.authoredValues(),
                customerConfiguration.derivedData()
        );
    }

    private static void requireAvailableBinding(@Nonnull List<CommunicationProviderBindingEntity> available)
            throws ResponseException {
        if (available.isEmpty()) {
            throw ResponseException.conflict(
                    "Für den Identitätsanbieter ist keine verwendbare Kommunikationsanbindung konfiguriert."
            );
        }
    }

    @Nonnull
    private IdentityCacheEntity getAuthenticatedIdentity(@Nonnull String identitySessionId,
                                                         @Nonnull Integer relatedProcessNodeId,
                                                         @Nonnull String identityId) throws ResponseException {
        return identityCacheRepository
                .findAllBySessionIdAndRelatedProcessNodeId(identitySessionId, relatedProcessNodeId)
                .stream()
                .filter(entity -> Objects.equals(entity.getIdentityId(), identityId))
                .filter(entity -> entity.getType() == IdentityType.IdentityProvider)
                .filter(entity -> entity.getIdentityData() != null)
                .findFirst()
                .orElseThrow(() -> ResponseException.notFound("Die authentifizierte Identität wurde nicht gefunden."));
    }

    public record SelectionState(
            boolean required,
            boolean ready,
            @Nullable Integer selectedBindingId,
            @Nonnull List<BindingChoice> choices,
            @Nullable GroupLayoutElement customerLayout,
            @Nonnull AuthoredElementValues customerData,
            @Nonnull DerivedRuntimeElementData derivedData
    ) {
    }

    public record BindingChoice(
            @Nonnull Integer id,
            @Nonnull String name,
            @Nonnull String description
    ) {
        static BindingChoice from(@Nonnull CommunicationProviderBindingEntity binding) {
            return new BindingChoice(binding.getId(), binding.getName(), binding.getDescription());
        }
    }
}
