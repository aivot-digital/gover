package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentProviderConfigurationService {
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public PaymentProviderConfigurationService(ElementDerivationService elementDerivationService) {
        this.elementDerivationService = elementDerivationService;
    }

    @Nonnull
    public DerivedRuntimeElementData deriveConfiguration(@Nonnull PaymentProviderEntity provider,
                                                         @Nonnull PaymentProviderDefinition definition) throws ResponseException {
        var layout = definition.getPaymentConfigLayout();
        if (layout == null) {
            throw ResponseException.internalServerError(
                    "Die Zahlungsanbieter-Definition %s (%s v%d) stellt kein Konfigurationslayout bereit.",
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
                        provider.getConfig(),
                        options
                ),
                new ElementDerivationLogger()
        );
    }
}
