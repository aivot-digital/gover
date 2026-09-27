package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageProviderConfigurationService {
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public StorageProviderConfigurationService(ElementDerivationService elementDerivationService) {
        this.elementDerivationService = elementDerivationService;
    }

    @Nonnull
    public DerivedRuntimeElementData deriveConfiguration(@Nonnull StorageProviderEntity provider,
                                                         @Nonnull StorageProviderDefinition<?> definition) throws ResponseException {
        var layout = definition.getProviderConfigLayout();
        if (layout == null) {
            throw ResponseException.internalServerError(
                    "Die Speicheranbieter-Definition %s (%s v%d) stellt kein Konfigurationslayout bereit.",
                    StringUtils.quote(definition.getName()),
                    StringUtils.quote(definition.getKey()),
                    definition.getMajorVersion()
            );
        }

        var options = new ElementDerivationOptions();
        options.setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS));

        return elementDerivationService.derive(
                new ElementDerivationRequest(
                        layout,
                        provider.getConfiguration(),
                        options
                )
        );
    }

    @Nonnull
    public <T> T mapToConfig(@Nonnull StorageProviderEntity provider,
                             @Nonnull StorageProviderDefinition<T> definition) throws ResponseException {
        var derivedConfiguration = deriveConfiguration(provider, definition);

        try {
            return ElementPOJOMapper
                    .mapToPOJO(derivedConfiguration.getEffectiveValues(), definition.getConfigClass());
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die Konfiguration des Speicheranbieters %s (ID %s) konnte nicht geladen werden. Die folgende Fehlermeldung wurde protokolliert: %s",
                    StringUtils.quote(provider.getName()),
                    StringUtils.quote(String.valueOf(provider.getId())),
                    e.getMessage()
            );
        }
    }
}
