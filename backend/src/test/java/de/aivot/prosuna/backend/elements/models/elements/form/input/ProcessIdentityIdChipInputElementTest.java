package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessIdentityIdChipInputElementTest {
    @Test
    void shouldAcceptAnyIdentityIdsWhenNoBoundsAreConfigured() {
        var element = new ProcessIdentityIdInputElement()
                .setPlaceholder("Identitäten auswählen");

        assertDoesNotThrow(() -> element.performValidation(List.of("citizen")));
    }

    @Test
    void shouldRejectTooFewIdentityIds() {
        var element = new ProcessIdentityIdInputElement()
                .setMinItems(2);

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of("citizen")));

        assertEquals("Mindestens 2 Einträge erforderlich.", exception.getMessage());
    }

    @Test
    void shouldRejectTooManyIdentityIds() {
        var element = new ProcessIdentityIdInputElement()
                .setMaxItems(1);

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of("citizen", "business")));

        assertEquals("Maximal 1 Einträge erlaubt.", exception.getMessage());
    }
}
