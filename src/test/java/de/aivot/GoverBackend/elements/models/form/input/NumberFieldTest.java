package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberFieldTest {

    @Test
    void validate() {
        var numberField = new NumberField(Map.of());

        assertDoesNotThrow(() -> numberField.validate(10.d));
        assertDoesNotThrow(() -> numberField.validate(10.1234d));

        assertThrows(ValidationException.class, () -> numberField.validate(Double.NaN));

        assertThrows(ValidationException.class, () -> numberField.validate(NumberField.AbsoluteMaxValue + 1));
        assertThrows(ValidationException.class, () -> numberField.validate(NumberField.AbsoluteMinValue - 1));
    }
}