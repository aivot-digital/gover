package de.aivot.GoverBackend.nocode.providers;

import de.aivot.GoverBackend.serviceprovider.providers.ServiceProvider;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;

/**
 * This interface is used to implement no code operator providers.
 * Operators will be grouped by the provider and can be used in the no code expressions.
 */
public interface NoCodeOperatorServiceProvider extends ServiceProvider {
    /**
     * Returns the operators provided by this provider.
     *
     * @return the operators
     */
    NoCodeOperator[] getOperators();
}
