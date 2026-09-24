package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentSelectInputElementTest {
    @Test
    void formatsAndValidatesPositiveIntegerIds() {
        var element = new DepartmentSelectInputElement();

        assertEquals(42, element.formatValue(42L));
        assertEquals(42, element.formatValue(" 42 "));
        assertNull(element.formatValue(" "));
        assertDoesNotThrow(() -> element.validate(42));
        assertThrows(ValidationException.class, () -> element.validate(0));
        assertThrows(ValidationException.class, () -> element.validate("invalid"));
        assertThrows(ValidationException.class, () -> element.validate(4.2));
    }

    @Test
    void evaluatesScalarDepartmentIds() {
        var element = new DepartmentSelectInputElement();

        assertTrue(element.evaluate(ConditionOperator.Equals, 42, "42"));
        assertFalse(element.evaluate(ConditionOperator.Empty, 42, null));
        assertTrue(element.evaluate(ConditionOperator.NotEmpty, 42, null));
    }

    @Test
    void roundTripsSelectionSettingsThroughBaseElementSerialization() throws Exception {
        var element = new DepartmentSelectInputElement()
                .setPlaceholder("Keine Organisationseinheit ausgewählt")
                .setDialogTitle("Organisationseinheit auswählen");

        var serialized = JsonMapperFactory.getInstance().writeValueAsString(element);
        var deserialized = JsonMapperFactory.getInstance().readValue(serialized, BaseElement.class);
        var selector = assertInstanceOf(DepartmentSelectInputElement.class, deserialized);

        assertEquals("Keine Organisationseinheit ausgewählt", selector.getPlaceholder());
        assertEquals("Organisationseinheit auswählen", selector.getDialogTitle());
    }
}
