package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.gover.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessDataKeyInputElementTest {
    @Test
    void validateShouldAcceptSupportedProcessDataKeys() {
        var element = new ProcessDataKeyInputElement();

        assertDoesNotThrow(() -> element.validate(" applicant.person_* "));
        assertEquals("applicant.person_*", element.formatValue(" applicant.person_* "));
    }

    @Test
    void validateShouldRejectUnsupportedCharacters() {
        var element = new ProcessDataKeyInputElement();

        var exception = assertThrows(ValidationException.class, () -> element.validate("applicant-name"));

        assertEquals("Der Prozessdaten-Schlüssel darf nur Buchstaben (A-Z), Zahlen, Punkte, Unterstriche und Sternchen enthalten.", exception.getMessage());
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(new ProcessDataKeyInputElement());

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        assertInstanceOf(ProcessDataKeyInputElement.class, deserialized);
    }
}
