package de.aivot.GoverBackend.plugins.core.v1.operators;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.date.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.list.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.math.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeConcatOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeRegexExtractOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.NoCodeSplitOperator;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class CommonOperatorsV1 implements NoCodeOperatorsProvider {
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
        return Core.PLUGIN_KEY;
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
                new NoCodeConcatOperator(),
                new NoCodeRegexExtractOperator(),
                new NoCodeRegexMatchOperator(),
                new NoCodeSplitOperator(),
        };
    }
}
