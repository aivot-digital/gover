package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeResultTest {
    @Test
    void getValueAsBooleanShouldReadConditionsLikeOperators() {
        assertTrue(new NoCodeResult(true).getValueAsBoolean());
        assertFalse(new NoCodeResult(null).getValueAsBoolean());
        assertFalse(new NoCodeResult("false").getValueAsBoolean());
        assertFalse(new NoCodeResult("falsch").getValueAsBoolean());
        assertTrue(new NoCodeResult(" wahr ").getValueAsBoolean());
    }

    @Test
    void getValueAsBooleanShouldRejectOtherValues() {
        for (var value : List.of(BigDecimal.ONE, BigDecimal.ZERO, "anything", Map.of(), Map.of("k", "v"))) {
            var error = assertThrows(IllegalStateException.class, () -> new NoCodeResult(value).getValueAsBoolean());
            assertEquals("Der Wert ist weder wahr noch falsch. Verwenden Sie eine Bedingung oder einen Wahrheitswert.", error.getMessage());
        }
    }
}
