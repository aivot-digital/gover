package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChipInputElementTest {
    @Test
    void normalizesScalarAndCollectionInputWithoutSplittingStoredValues() {
        var element = new ChipInputElement().setPasteSeparators(List.of(","));
        assertNull(element.formatValue(null));
        assertNull(element.formatValue(""));
        assertNull(element.formatValue(" \t\n "));
        assertNull(element.formatValue(List.of()));
        assertNull(element.formatValue(List.of(" ")));
        assertEquals(List.of("A"), element.formatValue(" A "));
        assertEquals(List.of("A", "B"), element.formatValue(Arrays.asList(null, " A ", "", "B")));
        assertEquals(List.of("A,B"), element.formatValue(" A,B "));
    }

    @Test
    void enforcesRequirednessAndOptionalMinimum() {
        var element = new ChipInputElement().setMinItems(2);
        assertDoesNotThrow(() -> element.validate(null));
        assertDoesNotThrow(() -> element.validate(" "));
        assertDoesNotThrow(() -> element.validate(List.of()));
        assertThrows(ValidationException.class, () -> element.validate(List.of("A")));
        assertDoesNotThrow(() -> element.validate(List.of("A", "B")));
        element.setRequired(true);
        assertThrows(ValidationException.class, () -> element.validate(null));
        assertThrows(ValidationException.class, () -> element.validate(""));
        assertThrows(ValidationException.class, () -> element.validate(" "));
        assertThrows(ValidationException.class, () -> element.validate(List.of(" ")));
    }

    @Test
    void enforcesCountLengthAndDuplicateRulesWithoutChangingValues() {
        var element = new ChipInputElement().setMaxItems(2).setMaxItemLength(3);
        assertDoesNotThrow(() -> element.validate(List.of("ABC", "DE")));
        assertDoesNotThrow(() -> element.validate(List.of(" ABC ")));
        assertThrows(ValidationException.class, () -> element.validate(List.of("ABCD")));
        assertThrows(ValidationException.class, () -> element.validate(List.of("A", "B", "C")));
        assertThrows(ValidationException.class, () -> element.validate(List.of("A", " A ")));
        element.setAllowDuplicates(true);
        assertDoesNotThrow(() -> element.validate(List.of("A", "A")));
        element.setMaxItems(0).setMaxItemLength(0);
        assertDoesNotThrow(() -> element.validate(List.of("ABCD", "B", "C")));
        assertEquals(List.of("ABCD", "B", "C"), element.formatValue(List.of("ABCD", "B", "C")));
    }

    @Test
    void handlesEmptyAndMembershipConditionsConsistently() {
        var element = new ChipInputElement();
        assertTrue(element.evaluate(ConditionOperator.Empty, null, null));
        assertTrue(element.evaluate(ConditionOperator.Empty, List.of(), null));
        assertTrue(element.evaluate(ConditionOperator.Empty, " ", null));
        assertFalse(element.evaluate(ConditionOperator.NotEmpty, List.of(), null));
        assertTrue(element.evaluate(ConditionOperator.NotEmpty, List.of("A"), null));
        assertTrue(element.evaluate(ConditionOperator.Includes, List.of("A", "B"), List.of("A", "B")));
        assertFalse(element.evaluate(ConditionOperator.Includes, List.of("A"), List.of("A", "B")));
        assertFalse(element.evaluate(ConditionOperator.NotIncludes, List.of("A"), List.of("A", "B")));
        assertTrue(element.evaluate(ConditionOperator.NotIncludes, List.of("A"), List.of("B", "C")));
        assertTrue(element.evaluate(ConditionOperator.NotIncludes, null, "A"));
        assertFalse(element.evaluate(ConditionOperator.Includes, null, "A"));
        assertTrue(element.evaluate(ConditionOperator.Includes, null, List.of()));
        assertTrue(element.evaluate(ConditionOperator.NotIncludes, null, List.of()));
        assertFalse(element.evaluate(ConditionOperator.Includes, List.of("A"), null));
        assertFalse(element.evaluate(ConditionOperator.NotIncludes, List.of("A"), null));
    }

    @Test
    void roundTripsNewOptionsAndPreservesLegacyDefaults() throws Exception {
        var mapper = JsonMapperFactory.getInstance();
        var element = new ChipInputElement().setMaxItemLength(30).setPasteSeparators(List.of("\n", ","));
        var serialized = mapper.writeValueAsString(element);
        var restored = assertInstanceOf(ChipInputElement.class, mapper.readValue(serialized, BaseElement.class));
        assertEquals(30, restored.getMaxItemLength());
        assertEquals(List.of("\n", ","), restored.getPasteSeparators());
        var equivalent = mapper.readValue(serialized, ChipInputElement.class);
        assertEquals(restored, equivalent);
        equivalent.setMaxItemLength(20);
        assertNotEquals(restored, equivalent);
        equivalent.setMaxItemLength(30).setPasteSeparators(List.of());
        assertNotEquals(restored, equivalent);
        var legacy = mapper.readValue("{\"type\": " + element.getType().getKey() + "}", ChipInputElement.class);
        assertNull(legacy.getMaxItemLength());
        assertNull(legacy.getPasteSeparators());
    }
}
