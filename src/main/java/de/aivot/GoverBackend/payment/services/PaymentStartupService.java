package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

@Component
public class PaymentStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final List<PaymentProviderDefinition> paymentProviderDefinitions;

    @Autowired
    public PaymentStartupService(List<PaymentProviderDefinition> paymentProviderDefinitions
    ) {
        this.paymentProviderDefinitions = paymentProviderDefinitions;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var existingDefinitionKeys = new HashMap<String, PaymentProviderDefinition>();

        // Check for duplicate keys
        for (var definition : paymentProviderDefinitions) {
            var key = definition.getKey();
            if (existingDefinitionKeys.containsKey(key)) {
                var error = String.format(
                        "Duplicate payment provider definition key found. The offending key is %s. The definition %s and %s are both using this key",
                        key,
                        definition.getClass().getName(),
                        existingDefinitionKeys.get(key).getClass().getName()
                );

                throw new IllegalStateException(error);
            }

            if (key.isBlank() || key.length() > 32) {
                var error = String.format(
                        "Payment provider definition key is invalid. The key %s of definition %s is either blank or longer than 32 characters",
                        key, definition.getClass().getName()
                );

                throw new IllegalStateException(error);
            }

            existingDefinitionKeys.put(key, definition);
        }
    }
}
