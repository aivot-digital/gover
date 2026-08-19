package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
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
        var element = new ProcessDataKeyInputElement()
                .setScopeProcessDataKeyInputElementId("containerKey");

        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(element);

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var processDataKeyInputElement = assertInstanceOf(ProcessDataKeyInputElement.class, deserialized);
        assertEquals("containerKey", processDataKeyInputElement.getScopeProcessDataKeyInputElementId());
    }
}
