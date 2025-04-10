package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class NoCodeCommonOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeEqualsOperator(),
                new NoCodeGreaterThanOperator(),
                new NoCodeGreaterThanOrEqualOperator(),
                new NoCodeIfOperator(),
                new NoCodeIsDefinedOperator(),
                new NoCodeIsInvisibleOperator(),
                new NoCodeIsUndefinedOperator(),
                new NoCodeIsVisibleOperator(),
                new NoCodeLessThanOperator(),
                new NoCodeLessThanOrEqualOperator(),
                new NoCodeNotEqualsOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.common";
    }

    @Override
    public String getLabel() {
        return "Allgemeine Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält allgemeine Operatoren.";
    }
}
