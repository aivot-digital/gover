package de.aivot.GoverBackend.core.operators;

import de.aivot.GoverBackend.core.operators.bool.NoCodeAndOperator;
import de.aivot.GoverBackend.core.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.core.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.core.operators.common.*;
import de.aivot.GoverBackend.core.operators.date.*;
import de.aivot.GoverBackend.core.operators.list.*;
import de.aivot.GoverBackend.core.operators.math.*;
import de.aivot.GoverBackend.core.operators.object.NoCodeObjectGetOperator;
import de.aivot.GoverBackend.core.operators.text.NoCodeRegexExtractOperator;
import de.aivot.GoverBackend.core.operators.text.NoCodeRegexMatchOperator;
import de.aivot.GoverBackend.core.operators.text.NoCodeSplitOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class NoCodeAivotOperatorServiceProvider implements NoCodeOperatorServiceProvider {
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

    @Override
    public String getPackageName() {
        return "aivot";
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
