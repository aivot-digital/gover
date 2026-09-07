package de.aivot.prosuna.backend.elements.models.elements.layout;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicatingContainerLayoutElementTest {
    @Test
    void evaluatesRowCountWithoutReinterpretingAuthoredOrEffectiveCells() {
        var element = new ReplicatingContainerLayoutElement();
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("name", "Ada");

        for (var row : List.of(
                new ReplicatingContainerLayoutElementValue()
                        .setValues(new AuthoredElementValues().putLiteral("name", "Ada")),
                new EffectiveReplicatingContainerLayoutElementValue().setValues(effectiveValues),
                Map.of("id", "row-1", "values", Map.of("name", "Ada"))
        )) {
            var rows = List.of(row);
            assertFalse(element.evaluate(ConditionOperator.Empty, rows, null));
            assertTrue(element.evaluate(ConditionOperator.NotEmpty, rows, null));
            assertTrue(element.evaluate(ConditionOperator.ReplicatingListLengthEquals, rows, 1));
            assertFalse(element.evaluate(ConditionOperator.ReplicatingListLengthGreaterThan, rows, 1));
        }

        assertTrue(element.evaluate(ConditionOperator.Empty, List.of(), null));
        assertTrue(element.evaluate(ConditionOperator.Empty, null, null));
    }
}
