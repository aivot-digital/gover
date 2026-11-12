package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeTextOperatorsTest {
    private static final NoCodeTextCompareOperator CONTAINS = new NoCodeTextCompareOperator(NoCodeTextCompareOperator.Mode.CONTAINS);
    private static final NoCodeTextCompareOperator STARTS_WITH = new NoCodeTextCompareOperator(NoCodeTextCompareOperator.Mode.STARTS_WITH);
    private static final NoCodeTextCompareOperator ENDS_WITH = new NoCodeTextCompareOperator(NoCodeTextCompareOperator.Mode.ENDS_WITH);
    private static final NoCodeTextCompareOperator EQUALS_IGNORE_CASE = new NoCodeTextCompareOperator(NoCodeTextCompareOperator.Mode.EQUALS_IGNORE_CASE);
    private static final NoCodeRegexFindOperator REGEX_FIND = new NoCodeRegexFindOperator();

    @Test
    void textComparisonsShouldTreatSearchTextLiterally() throws NoCodeException {
        var data = runtime();

        assertTrue(evaluate(CONTAINS, "Antrag (neu).pdf", "(neu)."));
        assertFalse(evaluate(CONTAINS, "Antrag neu", "(neu)"));
        // \E would end a \Q…\E quoted regex, which is why these operators do not use regex-match.
        assertTrue(evaluate(CONTAINS, "a\\Eb", "\\E"));
        assertTrue(evaluate(STARTS_WITH, "DE89 3704", "DE"));
        assertFalse(evaluate(STARTS_WITH, "de89 3704", "DE"));
        assertTrue(evaluate(ENDS_WITH, "bescheid.pdf", ".pdf"));
        assertFalse(evaluate(ENDS_WITH, "bescheid.pdfx", ".pdf"));
        assertTrue(evaluate(EQUALS_IGNORE_CASE, "Müller", "MÜLLER"));
        assertFalse(evaluate(EQUALS_IGNORE_CASE, "Müller", "Mueller"));
        assertThrows(NoCodeException.class, () -> CONTAINS.evaluate(data, "a"));
    }

    @Test
    void textComparisonsShouldTreatMissingValuesAsEmptyText() throws NoCodeException {
        assertFalse(evaluate(CONTAINS, null, "x"));
        assertTrue(evaluate(CONTAINS, "abc", null));
        assertTrue(evaluate(EQUALS_IGNORE_CASE, null, ""));
    }

    @Test
    void regexFindShouldMatchPartOfTheText() throws NoCodeException {
        assertTrue(evaluate(REGEX_FIND, "Aktenzeichen AZ-2026-17", "AZ-\\d{4}"));
        assertFalse(evaluate(REGEX_FIND, "Aktenzeichen fehlt", "AZ-\\d{4}"));
        assertTrue(evaluate(REGEX_FIND, "zeile 1\nzeile 2", "(?m)^zeile 2$"));
    }

    @Test
    void regexFindShouldRejectInvalidPatterns() {
        assertThrows(NoCodeException.class, () -> REGEX_FIND.evaluate(runtime(), "abc", "("));
    }

    /**
     * Every legacy text condition must be expressible with a single no-code operator, optionally negated.
     */
    @ParameterizedTest
    @CsvSource({
            "Antrag auf Förderung, Förderung",
            "Antrag auf Förderung, förderung",
            "Antrag auf Förderung, Antrag",
            "Antrag auf Förderung, rung",
            "Müller, MÜLLER",
            "Müller, Müller",
            "abc, ''",
            "'', abc",
            "a.b, .",
            "AZ-2026-17, AZ-\\d{4}",
            "AZ-2026-17, AZ-\\d{4}-\\d+",
            "AZ-2026-17, [a-z]+",
    })
    void shouldMatchLegacyTextConditions(String value, String compared) throws NoCodeException {
        var element = new TextInputElement();

        assertParity(element, ConditionOperator.Includes, CONTAINS, false, value, compared);
        assertParity(element, ConditionOperator.NotIncludes, CONTAINS, true, value, compared);
        assertParity(element, ConditionOperator.StartsWith, STARTS_WITH, false, value, compared);
        assertParity(element, ConditionOperator.NotStartsWith, STARTS_WITH, true, value, compared);
        assertParity(element, ConditionOperator.EndsWith, ENDS_WITH, false, value, compared);
        assertParity(element, ConditionOperator.NotEndsWith, ENDS_WITH, true, value, compared);
        assertParity(element, ConditionOperator.EqualsIgnoreCase, EQUALS_IGNORE_CASE, false, value, compared);
        assertParity(element, ConditionOperator.NotEqualsIgnoreCase, EQUALS_IGNORE_CASE, true, value, compared);
        assertParity(element, ConditionOperator.IncludesPattern, REGEX_FIND, false, value, compared);
        assertParity(element, ConditionOperator.NotIncludesPattern, REGEX_FIND, true, value, compared);
        assertParity(element, ConditionOperator.MatchesPattern, new NoCodeRegexMatchOperator(), false, value, compared);
        assertParity(element, ConditionOperator.NotMatchesPattern, new NoCodeRegexMatchOperator(), true, value, compared);
    }

    @Test
    void commonProviderShouldRegisterTheTextOperators() {
        var businessTime = new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC());
        var identifiers = Arrays.stream(new CommonOperatorsV1(null, null, businessTime).getOperators())
                .map(NoCodeOperator::getIdentifier)
                .collect(Collectors.toSet());

        assertTrue(identifiers.containsAll(Set.of(
                "text-contains",
                "text-starts-with",
                "text-ends-with",
                "text-equals-ignore-case",
                "regex-find"
        )));
    }

    private static void assertParity(
            TextInputElement element,
            ConditionOperator legacyOperator,
            NoCodeOperator operator,
            boolean negated,
            String value,
            String compared
    ) throws NoCodeException {
        var legacy = element.evaluate(legacyOperator, value, compared);
        var noCode = evaluate(operator, value, compared) != negated;

        assertEquals(legacy, noCode, legacyOperator + " for „" + value + "“ and „" + compared + "“");
    }

    private static boolean evaluate(NoCodeOperator operator, Object... args) throws NoCodeException {
        return operator.evaluate(runtime(), args).getValueAsBoolean();
    }
}
