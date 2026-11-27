package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class NumberInputElementTest {
    @Test
    void formatValueRoundsToTheConfiguredDecimalPlaces() {
        var element = new NumberInputElement().setDecimalPlaces(2);

        assertEquals(new BigDecimal("1.24"), element.formatValue("1.235"));
        assertEquals(new BigDecimal("1234.57"), element.formatValue("1.234,567"));
    }

    @Test
    void formatValueTreatsEnormousExponentsAsInvalidAndTinyValuesAsZero() {
        var element = new NumberInputElement().setDecimalPlaces(2);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertNull(element.formatValue("1e300000000"));
            assertNull(element.formatValue("1e999999999"));
            assertNull(element.formatValue(new BigDecimal("1e300000000")));
            assertNull(element.formatValue("1,5e300000000"));
            assertEquals(new BigDecimal("0.00"), element.formatValue("1e-300000000"));
            assertEquals(new BigDecimal("0.00"), element.formatValue("1e-999999999"));
        });
    }

    @Test
    void validateRejectsEnormousExponentsInRequiredFields() {
        var element = new NumberInputElement();
        element.setRequired(true);

        assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> assertThrows(ValidationException.class, () -> element.validate("1e300000000"))
        );
    }
}
