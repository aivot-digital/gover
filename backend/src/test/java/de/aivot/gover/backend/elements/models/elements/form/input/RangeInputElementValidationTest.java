package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateTimeRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.RangeInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.TimeRangeInputElement;
import de.aivot.gover.backend.exceptions.RequiredValidationException;
import de.aivot.gover.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeInputElementValidationTest {
    @Test
    void dateRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new DateRangeInputElement(), LocalDate.parse("2025-01-01"));
    }

    @Test
    void timeRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new TimeRangeInputElement(), LocalTime.parse("10:00:00"));
    }

    @Test
    void dateTimeRangeShouldRejectOpenRange() {
        assertOpenRangeRejected(new DateTimeRangeInputElement(), Instant.parse("2025-01-01T10:00:00Z"));
    }

    @Test
    void dateRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new DateRangeInputElement(), LocalDate.parse("2025-01-01"));
    }

    @Test
    void timeRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new TimeRangeInputElement(), LocalTime.parse("10:00:00"));
    }

    @Test
    void dateTimeRangeShouldRejectPartialRequiredRange() {
        assertPartialRequiredRangeRejected(new DateTimeRangeInputElement(), Instant.parse("2025-01-01T10:00:00Z"));
    }

    private static <T> void assertOpenRangeRejected(
            BaseInputElement<RangeInputElementValue<T>> element,
            T start
    ) {
        var exception = assertThrows(
                ValidationException.class,
                () -> element.performValidation(new RangeInputElementValue<>(start, null))
        );

        assertEquals("Bitte geben Sie sowohl den Start- als auch den Endwert an.", exception.getMessage());
    }

    private static <T> void assertPartialRequiredRangeRejected(
            BaseInputElement<RangeInputElementValue<T>> element,
            T start
    ) {
        element.setRequired(true);

        assertThrows(
                RequiredValidationException.class,
                () -> element.performValidation(new RangeInputElementValue<>(start, null))
        );
    }
}
