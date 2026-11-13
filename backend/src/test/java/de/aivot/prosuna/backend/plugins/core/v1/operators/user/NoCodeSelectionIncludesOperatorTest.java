package de.aivot.prosuna.backend.plugins.core.v1.operators.user;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeSelectionIncludesOperatorTest {
    private final NoCodeSelectionIncludesOperator operator = new NoCodeSelectionIncludesOperator();

    private static final List<Map<String, Object>> SELECTION = List.of(
            Map.of("type", "orgUnit", "id", "12"),
            Map.of("type", "user", "id", "abc", "label", "Erika Mustermann")
    );

    @Test
    void shouldCompareEntriesByTypeAndId() throws NoCodeException {
        assertTrue(evaluate(SELECTION, "orgUnit:12"));
        assertTrue(evaluate(SELECTION, " user:abc "));
        assertTrue(evaluate(SELECTION, Map.of("type", "user", "id", "abc")));
        assertTrue(evaluate(SELECTION, List.of("orgUnit:12", "user:abc")));
        assertFalse(evaluate(SELECTION, List.of("orgUnit:12", "team:7")));
        assertFalse(evaluate(SELECTION, "team:12"));
    }

    @Test
    void shouldBeFalseWhenEitherSideIsEmpty() throws NoCodeException {
        assertFalse(evaluate(null, "orgUnit:12"));
        assertFalse(evaluate(List.of(), "orgUnit:12"));
        assertFalse(evaluate(SELECTION, null));
        assertFalse(evaluate(SELECTION, "not-a-token"));
    }

    @Test
    void shouldUnwrapAssignmentContextValues() throws NoCodeException {
        var context = Map.of("domainAndUserSelection", SELECTION, "generalAssigneePreference", "any");

        assertTrue(evaluate(context, "user:abc"));
        assertFalse(evaluate(context, "user:xyz"));
    }

    @Test
    void shouldMatchLegacyDomainAndUserSelectConditions() throws NoCodeException {
        var element = new DomainAndUserSelectInputElement();
        var context = new AssignmentContextInputElement();
        var contextValue = Map.of("domainAndUserSelection", SELECTION);

        for (var compared : List.<Object>of("orgUnit:12", "team:7", List.of("orgUnit:12", "user:abc"), List.of("orgUnit:12", "team:7"))) {
            assertEquals(element.evaluate(ConditionOperator.Includes, SELECTION, compared), evaluate(SELECTION, compared), "Includes " + compared);
            assertEquals(context.evaluate(ConditionOperator.Includes, contextValue, compared), evaluate(contextValue, compared), "Includes " + compared);
        }

        // Legacy NotIncludes means "none of the entries": not(selection-includes) per entry, combined with "and".
        assertEquals(
                element.evaluate(ConditionOperator.NotIncludes, SELECTION, "team:7"),
                !evaluate(SELECTION, "team:7")
        );
    }

    @Test
    void shouldBeRegisteredInCommonOperators() {
        var operators = new CommonOperatorsV1(
                null,
                null,
                new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())
        ).getOperators();

        assertTrue(Arrays.stream(operators).anyMatch(NoCodeSelectionIncludesOperator.class::isInstance));
    }

    private boolean evaluate(Object selection, Object searched) throws NoCodeException {
        return operator.evaluate(runtime(), selection, searched).getValueAsBoolean();
    }
}
