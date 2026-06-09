package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessIdentityIdChipInputElementTest {
    @Test
    void shouldAcceptConfiguredIdentityIds() {
        var element = new ProcessIdentityIdInputElement()
                .setSuggestions(List.of("citizen", "business"));

        assertDoesNotThrow(() -> element.performValidation(List.of("citizen")));
    }

    @Test
    void shouldRejectUnknownIdentityIds() {
        var element = new ProcessIdentityIdInputElement()
                .setSuggestions(List.of("citizen", "business"));

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of("unknown")));

        assertEquals("Ungültige Auswahl: unknown", exception.getMessage());
    }

    @Test
    void shouldRejectMissingIdentitySuggestions() {
        var element = new ProcessIdentityIdInputElement();

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of("citizen")));

        assertEquals("Dieses Element hat keine Identitäten definiert.", exception.getMessage());
    }
}
