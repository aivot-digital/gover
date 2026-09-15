package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.enums.ConditionOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessIdentityIdInputElementTest {
    private final ProcessIdentityIdInputElement element = new ProcessIdentityIdInputElement();

    @Test
    void shouldFormatOnlyTrimmedScalarIdentityIds() {
        assertEquals("citizen", element.formatValue("  citizen  "));
        assertNull(element.formatValue("   "));
        assertNull(element.formatValue(List.of("citizen")));
        assertNull(element.formatValue(42));
        assertNull(element.formatValue(null));
    }

    @Test
    void shouldRenderScalarIdentityIds() {
        assertEquals("citizen", element.toDisplayValue(element.formatValue(" citizen ")));
        assertEquals("Keine Angabe", element.toDisplayValue(null));
        assertEquals("Keine Angabe", element.toDisplayValue(" "));
    }

    @Test
    void shouldEvaluateScalarSelectConditions() {
        assertTrue(element.evaluate(ConditionOperator.Equals, " citizen ", "citizen"));
        assertFalse(element.evaluate(ConditionOperator.NotEquals, "citizen", "citizen"));
        assertTrue(element.evaluate(ConditionOperator.NotEquals, "citizen", "business"));
        assertTrue(element.evaluate(ConditionOperator.Empty, " ", null));
        assertTrue(element.evaluate(ConditionOperator.NotEmpty, "citizen", null));
        assertFalse(element.evaluate(ConditionOperator.Includes, "citizen", "cit"));
    }
}
