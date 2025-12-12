package de.aivot.GoverBackend.plugins.corePlugin.components.operators;

import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.bool.NoCodeAndOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.common.*;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.date.*;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.list.*;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.math.*;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.object.NoCodeObjectGetOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.text.NoCodeRegexExtractOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.text.NoCodeRegexMatchOperator;
import de.aivot.GoverBackend.plugins.corePlugin.components.operators.text.NoCodeSplitOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class Operators implements NoCodeOperatorsProvider, PluginComponent {
    @Override
    public String getKey() {
        return "common";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Override
    public String getName() {
        return "Allgemeine Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Modul enthält allgemeine Operatoren.";
    }

    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                // Bool
                new NoCodeAndOperator(),
                new NoCodeNotOperator(),
                new NoCodeOrOperator(),

                // Common
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
                new NoCodeValueOperator(),

                // Date
                new NoCodeAddToDateOperator(),
                new NoCodeCreateDateOperator(),
                new NoCodeCreateTimeOperator(),
                new NoCodeCreateTodayOperator(),
                new NoCodeSubtractFromDateOperator(),

                // List
                new NoCodeListAvgOperator(),
                new NoCodeListConcatOperator(),
                new NoCodeListContainsOperator(),
                new NoCodeListGetOperator(),
                new NoCodeListLengthOperator(),
                new NoCodeListSelectOperator(),
                new NoCodeListSumOperator(),

                // Math
                new NoCodeAddOperator(),
                new NoCodeDivideOperator(),
                new NoCodeMultiplyOperator(),
                new NoCodeRoundDownOperator(),
                new NoCodeRoundUpOperator(),
                new NoCodeSubtractOperator(),

                // Object
                new NoCodeObjectGetOperator(),

                // Text
                new NoCodeRegexExtractOperator(),
                new NoCodeRegexMatchOperator(),
                new NoCodeSplitOperator(),
        };
    }
}
