package de.aivot.prosuna.backend.plugins.core.v1.operators;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.date.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.list.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.object.NoCodeObjectGetOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.phone.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.secrets.NoCodeSecretsGetOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.*;
import de.aivot.prosuna.backend.plugins.core.v1.operators.user.NoCodeUserEmailOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.user.NoCodeUserFullNameOperator;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Prosuna.
 */
@Component
public class CommonOperatorsV1 implements NoCodeOperatorsProvider {
    @Nullable
    private final UserRepository userRepository;
    @Nullable
    private final SecretService secretService;
    private final BusinessTime businessTime;

    @Autowired
    public CommonOperatorsV1(
            @Nullable UserRepository userRepository,
            @Nullable SecretService secretService,
            BusinessTime businessTime
    ) {
        this.userRepository = userRepository;
        this.secretService = secretService;
        this.businessTime = businessTime;
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
    public String getAbstract() {
        return "Dieses Modul enthält allgemeine Operatoren.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Stellt die grundlegenden Operatoren für den No-Code-Ausdruckseditor von Prosuna bereit.

                Enthalten sind Operatoren für boolesche Logik, Vergleiche, Datums- und Zeitwerte, Listen, Mathematik, Objekte, Telefonnummern, Geheimnisse, Texte und Benutzerdaten. Sie können miteinander kombiniert werden, um Werte ohne eigenen Programmcode zu prüfen, umzuwandeln und zu berechnen.
                """;
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
                new NoCodeCombineDateAndTimeOperator(businessTime),
                new NoCodeCreateDateOperator(),
                new NoCodeCreateNowOperator(businessTime),
                new NoCodeCreateTimeOperator(),
                new NoCodeCreateTodayOperator(businessTime),
                new NoCodeExtractDateTimePartOperator(NoCodeExtractDateTimePartOperator.Part.DATE, businessTime),
                new NoCodeExtractDateTimePartOperator(NoCodeExtractDateTimePartOperator.Part.TIME, businessTime),
                new NoCodeFormatDateOperator(businessTime),
                new NoCodeFormatDateTimeOperator(businessTime),
                new NoCodeFormatTimeOperator(),
                new NoCodeSubtractFromDateOperator(),
                new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATE),
                new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.TIME),
                new NoCodeTemporalCompareOperator(NoCodeTemporalCompareOperator.TemporalType.DATETIME),

                // List
                new NoCodeListAvgOperator(),
                new NoCodeListConcatOperator(),
                new NoCodeListContainsOperator(),
                new NoCodeListCountOperator(),
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

                // Phone
                new NoCodePhoneNumberIsValidOperator(),
                new NoCodePhoneNumberIsPossibleOperator(),
                new NoCodePhoneNumberNormalizeOperator(),

                // Secrets
                new NoCodeSecretsGetOperator(secretService),

                // Text
                new NoCodeBase64Operator(),
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
