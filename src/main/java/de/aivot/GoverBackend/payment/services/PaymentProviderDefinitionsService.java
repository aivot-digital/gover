package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentProviderDefinitionsService {
    private final Map<String, PaymentProviderDefinition> paymentProviderDefinitionMap;

    @Autowired
    public PaymentProviderDefinitionsService(
            List<PaymentProviderDefinition> paymentProviderDefinitions
    ) {
        this.paymentProviderDefinitionMap = new HashMap<>();
        for (var definition : paymentProviderDefinitions) {
            paymentProviderDefinitionMap.put(definition.getKey(), definition);
        }
    }

    @Nonnull
    public Optional<PaymentProviderDefinition> getProviderDefinition(@Nonnull String providerKey) {
        if (paymentProviderDefinitionMap.containsKey(providerKey)) {
            return Optional.of(paymentProviderDefinitionMap.get(providerKey));
        } else {
            return Optional.empty();
        }
    }
}
