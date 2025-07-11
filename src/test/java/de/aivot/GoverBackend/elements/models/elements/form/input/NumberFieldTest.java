package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberFieldTest {

    @Test
    void performValidation() {
        var numberField = new NumberField(Map.of());

        assertDoesNotThrow(() -> numberField.performValidation(10.d));
        assertDoesNotThrow(() -> numberField.performValidation(10.1234d));

        assertThrows(ValidationException.class, () -> numberField.performValidation(Double.NaN));

        assertThrows(ValidationException.class, () -> numberField.performValidation(NumberField.AbsoluteMaxValue + 1));
        assertThrows(ValidationException.class, () -> numberField.performValidation(NumberField.AbsoluteMinValue - 1));
    }
}