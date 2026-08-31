package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderBindingRepository;
import de.aivot.prosuna.backend.communication.repositories.CommunicationProviderRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CommunicationService {
    private final CommunicationProviderBindingRepository bindingRepository;
    private final CommunicationProviderRepository providerRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final CommunicationProviderDefinitionService definitionService;
    private final CommunicationProviderConfigurationService configurationService;
    private final DefaultMailCommunicationService defaultMailCommunicationService;

    public CommunicationService(CommunicationProviderBindingRepository bindingRepository,
                                CommunicationProviderRepository providerRepository,
                                IdentityProviderRepository identityProviderRepository,
                                CommunicationProviderDefinitionService definitionService,
                                CommunicationProviderConfigurationService configurationService,
                                DefaultMailCommunicationService defaultMailCommunicationService) {
        this.bindingRepository = bindingRepository;
        this.providerRepository = providerRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.definitionService = definitionService;
        this.configurationService = configurationService;
        this.defaultMailCommunicationService = defaultMailCommunicationService;
    }

    /**
     * Sends through the binding selected while the identity was authenticated. This is the only
     * runtime entry point that invokes a communication-provider definition.
     */
    public void sendMessage(@Nonnull IdentityData identityData, @Nonnull CommunicationMessage message) {
        if (identityData.type() == IdentityType.Email) {
            var emailAddress = identityData.emailAddress();
            if (emailAddress == null) {
                throw new CommunicationException(
                        "Für die E-Mail-Identität %s ist keine E-Mail-Adresse verfügbar.",
                        identityData.identityId()
                );
            }
            defaultMailCommunicationService.sendMessage(emailAddress, message);
            return;
        }

        var resolved = resolveSelected(identityData);
        sendResolved(resolved, identityData, message);
    }

    @Nonnull
    public List<CommunicationProviderBindingEntity> getAvailableBindings(@Nonnull IdentityData identityData) {
        requireIdentityProviderIdentity(identityData);
        var identityProvider = loadIdentityProvider(identityData.providerKey(), identityData.identityId());
        requireIdentityProviderEnabled(identityProvider);
        return getUsableBindings(identityProvider);
    }

    /**
     * Resolves every currently usable binding for an identity provider. The provider itself may be
     * disabled while it is being configured; callers enforce the enabled-provider invariant where
     * appropriate.
     */
    @Nonnull
    public List<CommunicationProviderBindingEntity> getUsableBindings(@Nonnull IdentityProviderEntity identityProvider) {
        return bindingRepository
                .findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(identityProvider.getKey())
                .stream()
                .filter(binding -> {
                    try {
                        resolve(binding, identityProvider);
                        return true;
                    } catch (CommunicationException ignored) {
                        return false;
                    }
                })
                .toList();
    }

    @Nonnull
    public CustomerConfiguration getCustomerConfiguration(@Nonnull IdentityData identityData) {
        var resolved = resolveSelected(identityData);
        return getCustomerConfigurationResolved(resolved, identityData);
    }

    public boolean isCustomerConfigurationReady(@Nonnull IdentityData identityData) {
        if (identityData.communicationProviderBindingId() == null) {
            return false;
        }
        try {
            return getCustomerConfiguration(identityData).ready();
        } catch (CommunicationException e) {
            return false;
        }
    }

    @Nonnull
    private ResolvedCommunicationProvider resolveSelected(@Nonnull IdentityData identityData) {
        requireIdentityProviderIdentity(identityData);
        var bindingId = identityData.communicationProviderBindingId();
        if (bindingId == null) {
            throw new CommunicationException(
                    "Für die Identität %s wurde kein Kommunikationsanbieter ausgewählt.",
                    identityData.identityId()
            );
        }

        var binding = bindingRepository
                .findById(bindingId)
                .orElseThrow(() -> new CommunicationException(
                        "Die ausgewählte Kommunikationsanbindung mit der ID %d existiert nicht.",
                        bindingId
                ));
        if (!Objects.equals(binding.getIdentityProviderKey(), identityData.providerKey())) {
            throw new CommunicationException(
                    "Die Kommunikationsanbindung %s gehört nicht zum Nutzerkontenanbieter der Identität %s.",
                    binding.getName(),
                    identityData.identityId()
            );
        }
        var identityProvider = loadIdentityProvider(identityData.providerKey(), identityData.identityId());
        requireIdentityProviderEnabled(identityProvider);
        return resolve(binding, identityProvider);
    }

    @Nonnull
    private ResolvedCommunicationProvider resolve(@Nonnull CommunicationProviderBindingEntity binding,
                                                  @Nonnull IdentityProviderEntity identityProvider) {
        if (!binding.getEnabled()) {
            throw new CommunicationException("Die Kommunikationsanbindung %s ist deaktiviert.", binding.getName());
        }
        if (!Objects.equals(binding.getIdentityProviderKey(), identityProvider.getKey())) {
            throw new CommunicationException(
                    "Die Kommunikationsanbindung %s gehört nicht zum Nutzerkontenanbieter %s.",
                    binding.getName(),
                    identityProvider.getName()
            );
        }

        var provider = providerRepository
                .findById(binding.getCommunicationProviderId())
                .orElseThrow(() -> new CommunicationException(
                        "Der Kommunikationsanbieter mit der ID %d existiert nicht.",
                        binding.getCommunicationProviderId()
                ));
        if (!provider.getEnabled()) {
            throw new CommunicationException("Der Kommunikationsanbieter %s ist deaktiviert.", provider.getName());
        }

        if (!Objects.equals(provider.getTestProvider(), identityProvider.getIsTestProvider())) {
            throw new CommunicationException(
                    "Test- und Produktivsysteme dürfen für die Kommunikationsanbindung %s nicht gemischt werden.",
                    binding.getName()
            );
        }

        var definition = definitionService
                .retrieveProviderDefinition(
                        provider.getCommunicationProviderDefinitionKey(),
                        provider.getCommunicationProviderDefinitionVersion()
                )
                .orElseThrow(() -> new CommunicationException(
                        "Die Definition %s in Version %d des Kommunikationsanbieters %s ist nicht verfügbar.",
                        provider.getCommunicationProviderDefinitionKey(),
                        provider.getCommunicationProviderDefinitionVersion(),
                        provider.getName()
                ));
        if (!definition.supportsIdentityProvider(identityProvider)) {
            throw new CommunicationException(
                    "Der Kommunikationsanbieter %s unterstützt den Nutzerkontenanbieter %s nicht.",
                    provider.getName(),
                    identityProvider.getName()
            );
        }

        return resolveTyped(provider, identityProvider, binding, definition);
    }

    @Nonnull
    private IdentityProviderEntity loadIdentityProvider(@Nullable UUID identityProviderKey,
                                                        @Nonnull String identityId) {
        if (identityProviderKey == null) {
            throw new CommunicationException(
                    "Für die Identität %s ist kein Nutzerkontenanbieter hinterlegt.",
                    identityId
            );
        }
        try {
            return identityProviderRepository
                    .findById(identityProviderKey)
                    .orElseThrow(() -> new CommunicationException(
                            "Der Nutzerkontenanbieter der Identität %s existiert nicht.",
                            identityId
                    ));
        } catch (CommunicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommunicationException(
                    "Der Nutzerkontenanbieter der Identität %s konnte nicht geladen werden."
                            .formatted(identityId),
                    e
            );
        }
    }

    private static void requireIdentityProviderEnabled(@Nonnull IdentityProviderEntity identityProvider) {
        if (!Boolean.TRUE.equals(identityProvider.getIsEnabled())) {
            throw new CommunicationException("Der Nutzerkontenanbieter %s ist deaktiviert.", identityProvider.getName());
        }
    }

    private static void requireIdentityProviderIdentity(@Nonnull IdentityData identityData) {
        if (identityData.type() != IdentityType.IdentityProvider) {
            throw new CommunicationException(
                    "Für die E-Mail-Identität %s stehen keine Kommunikationsanbieter zur Auswahl.",
                    identityData.identityId()
            );
        }
    }

    @Nonnull
    private <C, I> ResolvedCommunicationProvider resolveTyped(
            @Nonnull CommunicationProviderEntity provider,
            @Nonnull IdentityProviderEntity identityProvider,
            @Nonnull CommunicationProviderBindingEntity binding,
            @Nonnull CommunicationProviderDefinition<C, I> definition
    ) {
        var providerConfiguration = configurationService.mapProviderConfiguration(provider, definition);
        var bindingConfiguration = configurationService.mapBindingConfiguration(binding, identityProvider, definition);
        return new ResolvedCommunicationProvider(
                definition,
                new CommunicationProviderContext<>(
                        provider,
                        identityProvider,
                        binding,
                        providerConfiguration,
                        bindingConfiguration
                )
        );
    }

    @SuppressWarnings("unchecked")
    private <C, I> void sendResolved(@Nonnull ResolvedCommunicationProvider resolved,
                                     @Nonnull IdentityData identityData,
                                     @Nonnull CommunicationMessage message) {
        var definition = (CommunicationProviderDefinition<C, I>) resolved.definition();
        var context = (CommunicationProviderContext<C, I>) resolved.context();
        try {
            definition.sendMessage(context, identityData, message);
        } catch (CommunicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommunicationException(
                    "Der Kommunikationsanbieter %s konnte die Nachricht nicht versenden."
                            .formatted(context.communicationProvider().getName()),
                    e
            );
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <C, I> CustomerConfiguration getCustomerConfigurationResolved(
            @Nonnull ResolvedCommunicationProvider resolved,
            @Nonnull IdentityData identityData
    ) {
        var definition = (CommunicationProviderDefinition<C, I>) resolved.definition();
        var context = (CommunicationProviderContext<C, I>) resolved.context();

        final GroupLayoutElement layout;
        try {
            layout = definition.getCustomerLayout(context, identityData);
        } catch (Exception e) {
            throw new CommunicationException(
                    "Die Kundeneingaben für den Kommunikationsanbieter %s konnten nicht geladen werden."
                            .formatted(context.communicationProvider().getName()),
                    e
            );
        }

        if (layout == null) {
            return new CustomerConfiguration(null, DerivedRuntimeElementData.empty(), true);
        }

        var authoredValues = new AuthoredElementValues();
        authoredValues.putAll(identityData.communicationProviderData());
        var derived = configurationService.deriveCustomerData(layout, authoredValues);
        return new CustomerConfiguration(layout, derived, !derived.hasAnyError());
    }

    public record CustomerConfiguration(
            @Nullable GroupLayoutElement layout,
            @Nonnull DerivedRuntimeElementData derivedData,
            boolean ready
    ) {
    }

    private record ResolvedCommunicationProvider(
            @Nonnull CommunicationProviderDefinition<?, ?> definition,
            @Nonnull CommunicationProviderContext<?, ?> context
    ) {
    }
}
