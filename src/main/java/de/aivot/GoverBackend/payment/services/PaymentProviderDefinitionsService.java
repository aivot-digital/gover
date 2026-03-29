package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentProviderDefinitionsService {
    private final Map<String, Map<Integer, PaymentProviderDefinition>> paymentProviderDefinitionMap;

    @Autowired
    public PaymentProviderDefinitionsService(
            List<PaymentProviderDefinition> paymentProviderDefinitions
    ) {
        this.paymentProviderDefinitionMap = paymentProviderDefinitions
                .stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                PaymentProviderDefinition::getKey,
                                java.util.stream.Collectors.toMap(
                                        PaymentProviderDefinition::getMajorVersion,
                                        definition -> definition
                                )
                        )
                );
    }

    @Nonnull
    public Optional<PaymentProviderDefinition> getProviderDefinition(@Nonnull String providerKey, @Nonnull Integer providerVersion) {
        var providerVersions = paymentProviderDefinitionMap.get(providerKey);
        if (providerVersions == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(providerVersions.get(providerVersion));
    }

    @Nonnull
    public Optional<PaymentProviderDefinition> getLatestProviderDefinition(@Nonnull String providerKey) {
        var providerVersions = paymentProviderDefinitionMap.get(providerKey);
        if (providerVersions == null || providerVersions.isEmpty()) {
            return Optional.empty();
        }

        return providerVersions
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue);
    }
}
