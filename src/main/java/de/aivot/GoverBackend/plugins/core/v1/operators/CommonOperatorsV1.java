package de.aivot.GoverBackend.plugins.core.v1.operators;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.date.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.list.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.math.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.secrets.NoCodeSecretsGetOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.text.*;
import de.aivot.GoverBackend.plugins.core.v1.operators.user.NoCodeUserEmailOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.user.NoCodeUserFullNameOperator;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class CommonOperatorsV1 implements NoCodeOperatorsProvider {
    @Nullable
    private final UserRepository userRepository;
    @Nullable
    private final SecretService secretService;

    @Autowired
    public CommonOperatorsV1(@Nullable UserRepository userRepository, @Nullable SecretService secretService) {
        this.userRepository = userRepository;
        this.secretService = secretService;
    }

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
        return CorePlugin.PLUGIN_KEY;
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
                new NoCodeFormatDateOperator(),
                new NoCodeSubtractFromDateOperator(),

                // List
                new NoCodeListAvgOperator(),
                new NoCodeListConcatOperator(),
                new NoCodeListContainsOperator(),
                new NoCodeListGetOperator(),
                new NoCodeListIntersectionOperator(),
                new NoCodeListLengthOperator(),
                new NoCodeListOverlapsOperator(),
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

                // User
                new NoCodeUserEmailOperator(userRepository),
                new NoCodeUserFullNameOperator(userRepository),
        };
    }
}
