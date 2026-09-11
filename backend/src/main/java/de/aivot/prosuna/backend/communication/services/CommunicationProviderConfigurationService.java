package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

/** Maps authored provider and binding values through the normal element derivation pipeline. */
@Service
public class CommunicationProviderConfigurationService {
    private final ElementDerivationService elementDerivationService;

    public CommunicationProviderConfigurationService(ElementDerivationService elementDerivationService) {
        this.elementDerivationService = elementDerivationService;
    }

    @Nonnull
    public <C> C mapProviderConfiguration(@Nonnull CommunicationProviderEntity provider,
                                          @Nonnull CommunicationProviderDefinition<C, ?> definition) throws CommunicationException {
        var providerReference = formatReference(provider.getName(), provider.getId());
        try {
            return map(
                    definition.getConfigLayout(),
                    provider.getConfiguration(),
                    definition.getConfigClass(),
                    "Die Konfiguration des Kommunikationsanbieters %s ist ungültig."
                            .formatted(providerReference)
            );
        } catch (CommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new CommunicationException(
                    "Die Konfiguration des Kommunikationsanbieters %s konnte nicht geladen werden."
                            .formatted(providerReference),
                    e
            );
        }
    }

    @Nonnull
    public <I> I mapBindingConfiguration(@Nonnull CommunicationProviderBindingEntity binding,
                                         @Nonnull IdentityProviderEntity identityProvider,
                                         @Nonnull CommunicationProviderDefinition<?, I> definition) throws CommunicationException {
        var bindingReference = formatReference(binding.getName(), binding.getId());
        try {
            return map(
                    definition.getIdentityProviderBindingConfigLayout(identityProvider),
                    binding.getConfiguration(),
                    definition.getIdentityProviderBindingConfigClass(),
                    "Die Konfiguration der Kommunikationsanbindung %s ist ungültig."
                            .formatted(bindingReference)
            );
        } catch (CommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new CommunicationException(
                    "Die Konfiguration der Kommunikationsanbindung %s konnte nicht geladen werden."
                            .formatted(bindingReference),
                    e
            );
        }
    }

    @Nonnull
    public DerivedRuntimeElementData deriveCustomerData(@Nonnull BaseElement layout,
                                                        @Nonnull AuthoredElementValues values) {
        return deriveCustomerData(layout, values, new ElementDerivationOptions());
    }

    @Nonnull
    public DerivedRuntimeElementData deriveCustomerData(@Nonnull BaseElement layout,
                                                        @Nonnull AuthoredElementValues values,
                                                        @Nonnull ElementDerivationOptions options) {
        return elementDerivationService.derive(new ElementDerivationRequest(layout, values, options));
    }

    @Nonnull
    private <T> T map(@Nonnull BaseElement layout,
                      @Nonnull AuthoredElementValues values,
                      @Nonnull Class<T> targetClass,
                      @Nonnull String validationMessage) throws ElementDataConversionException, CommunicationException {
        var derived = elementDerivationService.derive(new ElementDerivationRequest(layout, values));
        if (derived.hasAnyError()) {
            throw new CommunicationException(validationMessage);
        }
        return ElementPOJOMapper.mapToPOJO(derived.getEffectiveValues(), targetClass);
    }

    @Nonnull
    private static String formatReference(@Nonnull String name, Integer id) {
        return id == null ? name : "%s (ID %d)".formatted(name, id);
    }
}
