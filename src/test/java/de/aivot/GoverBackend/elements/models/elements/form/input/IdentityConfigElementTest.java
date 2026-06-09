package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityConfigElementTest {
    private static final String MISSING_OPTION_MESSAGE = "Für jede Identität muss mindestens ein Identitätsanbieter ausgewählt werden.";

    @Test
    void shouldRejectSlotWithoutSelectedOption() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of());

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldRejectSlotWithNullOptions() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot();

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldRejectSlotWithoutIdentityProviderKey() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()));

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldAcceptSlotWithSelectedOption() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(UUID.randomUUID())));

        assertDoesNotThrow(() -> element.performValidation(List.of(slot)));
    }
}
