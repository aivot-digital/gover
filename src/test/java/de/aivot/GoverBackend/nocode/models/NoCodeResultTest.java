package de.aivot.GoverBackend.nocode.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeResultTest {
    @Test
    void getValueAsBooleanShouldHandleNumericAndStringValuesConsistently() {
        assertTrue(new NoCodeResult(BigDecimal.ONE).getValueAsBoolean());
        assertFalse(new NoCodeResult(BigDecimal.ZERO).getValueAsBoolean());
        assertFalse(new NoCodeResult("false").getValueAsBoolean());
        assertFalse(new NoCodeResult("falsch").getValueAsBoolean());
        assertTrue(new NoCodeResult("anything").getValueAsBoolean());
    }

    @Test
    void getValueAsBooleanShouldHandleMaps() {
        assertFalse(new NoCodeResult(Map.of()).getValueAsBoolean());
        assertTrue(new NoCodeResult(Map.of("k", "v")).getValueAsBoolean());
    }
}
