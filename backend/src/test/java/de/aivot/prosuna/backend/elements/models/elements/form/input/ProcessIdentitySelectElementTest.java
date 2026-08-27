package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessIdentitySelectElementTest {
    @Test
    void shouldNormalizeSupportedValues() {
        var element = new ProcessIdentitySelectElement();

        assertEquals(List.of("citizen"), element.formatValue(" citizen "));
        assertEquals(List.of("citizen", "business"), element.formatValue(List.of(" citizen ", "", "business")));
        assertNull(element.formatValue(" "));
        assertNull(element.formatValue(List.of(" ")));
    }

    @Test
    void shouldEnforceConfiguredBounds() {
        var element = new ProcessIdentitySelectElement()
                .setMinItems(2)
                .setMaxItems(3);

        assertDoesNotThrow(() -> element.performValidation(List.of("citizen", "business")));
        assertEquals(
                "Mindestens 2 Einträge erforderlich.",
                assertThrows(ValidationException.class, () -> element.performValidation(List.of("citizen"))).getMessage()
        );
        assertEquals(
                "Maximal 3 Einträge erlaubt.",
                assertThrows(ValidationException.class, () -> element.performValidation(List.of("a", "b", "c", "d"))).getMessage()
        );
    }

    @Test
    void shouldRejectDuplicateIdentityIds() {
        var element = new ProcessIdentitySelectElement();

        var exception = assertThrows(
                ValidationException.class,
                () -> element.performValidation(List.of("citizen", "citizen"))
        );

        assertEquals("Mehrfach vorhandene Einträge sind nicht erlaubt.", exception.getMessage());
    }

    @Test
    void shouldEvaluateListConditions() {
        var element = new ProcessIdentitySelectElement();

        assertEquals(true, element.evaluate(ConditionOperator.Includes, List.of("citizen", "business"), "citizen"));
        assertEquals(true, element.evaluate(ConditionOperator.NotIncludes, List.of("citizen"), "business"));
        assertEquals(true, element.evaluate(ConditionOperator.Empty, null, null));
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var element = new ProcessIdentitySelectElement()
                .setPlaceholder("Identität auswählen")
                .setMinItems(1)
                .setMaxItems(2);

        var serialized = JsonMapperFactory
                .getInstance()
                .writeValueAsString(element);
        var deserialized = JsonMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var identitySelectElement = assertInstanceOf(ProcessIdentitySelectElement.class, deserialized);
        assertEquals("Identität auswählen", identitySelectElement.getPlaceholder());
        assertEquals(1, identitySelectElement.getMinItems());
        assertEquals(2, identitySelectElement.getMaxItems());
    }
}
