package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This service evaluates no code expressions. It uses the operators provided by the registered {@link NoCodeOperatorsProvider} to evaluate the expressions.
 */
@Service
public class NoCodeOperatorProviderService {
    private static final Logger logger = LoggerFactory.getLogger(NoCodeOperatorProviderService.class);

    private final Map<String, NoCodeOperator> noCodeOperatorProviders;

    @Autowired
    public NoCodeOperatorProviderService(List<NoCodeOperatorsProvider> noCodeOperatorProviders) {
        this.noCodeOperatorProviders = new HashMap<>();

        for (var provider : noCodeOperatorProviders) {
            for (var operator : provider.getOperators()) {
                var packageScopedOperatorIdentifier = operator.getIdentifier();
                if (this.noCodeOperatorProviders.containsKey(packageScopedOperatorIdentifier)) {
                    logger
                            .atWarn()
                            .setMessage("No code operator with identifier " + packageScopedOperatorIdentifier + " already exists. Skipping this operator.")
                            .addKeyValue("operatorIdentifier", packageScopedOperatorIdentifier)
                            .addKeyValue("providerClassName", provider.getClass().getName())
                            .addKeyValue("operatorClassName", operator.getClass().getName())
                            .addKeyValue("operatorPackageName", operator.getClass().getPackageName())
                            .log();
                } else {
                    this.noCodeOperatorProviders.put(packageScopedOperatorIdentifier, operator);
                }
            }
        }
    }

    public Optional<NoCodeOperator> getNoCodeOperator(@Nullable String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }

        var operator = noCodeOperatorProviders.get(identifier);
        if (operator == null) {
            return Optional.empty();
        }

        return Optional.of(operator);
    }
}
