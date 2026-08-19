package de.aivot.prosuna.backend.nocode.providers;

import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;

/**
 * This interface is used to implement no code operator providers.
 * Operators will be grouped by the provider and can be used in the no code expressions.
 */
public interface NoCodeOperatorsProvider extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.OperatorProvider;
    }

    /**
     * Returns the operators provided by this provider.
     *
     * @return the operators
     */
    NoCodeOperator[] getOperators();
}
