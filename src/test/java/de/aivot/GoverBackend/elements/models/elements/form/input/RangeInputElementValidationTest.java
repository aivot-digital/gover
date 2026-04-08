package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeInputElementValidationTest {
    private static final ZonedDateTime START = ZonedDateTime.parse("2025-01-01T10:00:00Z");

    @Test
    void dateRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new DateRangeInputElement());
    }

    @Test
    void timeRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new TimeRangeInputElement());
    }

    @Test
    void dateTimeRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new DateTimeRangeInputElement());
    }

    @Test
    void dateRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new DateRangeInputElement());
    }

    @Test
    void timeRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new TimeRangeInputElement());
    }

    @Test
    void dateTimeRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new DateTimeRangeInputElement());
    }

    private static void assertOpenRangeRejected(BaseInputElement<RangeInputElementValue> element) {
        var exception = assertThrows(ValidationException.class, () -> element.performValidation(new RangeInputElementValue(START, null)));

        assertEquals("Bitte geben Sie sowohl den Start- als auch den Endwert an.", exception.getMessage());
    }

    private static void assertPartialRequiredRangeRejected(BaseInputElement<RangeInputElementValue> element) {
        element.setRequired(true);

        assertThrows(RequiredValidationException.class, () -> element.performValidation(new RangeInputElementValue(START, null)));
    }
}
