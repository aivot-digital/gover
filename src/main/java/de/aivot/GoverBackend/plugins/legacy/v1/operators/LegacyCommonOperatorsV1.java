package de.aivot.GoverBackend.plugins.legacy.v1.operators;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.GoverBackend.plugins.legacy.Legacy;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class LegacyCommonOperatorsV1 implements NoCodeOperatorsProvider {
    @Override
    public @Nonnull String getComponentKey() {
        return "common";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Legacy.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Allgemeine Operatoren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Modul enthält allgemeine Operatoren.";
    }

    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{

        };
    }
}
