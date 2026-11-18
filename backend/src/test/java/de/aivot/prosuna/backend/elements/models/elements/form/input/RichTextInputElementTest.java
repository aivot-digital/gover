package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RichTextInputElementTest {
    @Test
    void acceptsMarkdownUpToTheMaximumLength() {
        var element = new RichTextInputElement()
                .setMaxCharacters(8);

        assertDoesNotThrow(() -> element.validate("**Fett**"));
    }

    @Test
    void rejectsMarkdownAboveTheMaximumLengthIncludingFormattingCharacters() {
        var element = new RichTextInputElement()
                .setMaxCharacters(7);

        var exception = assertThrows(ValidationException.class, () -> element.validate("**Fett**"));

        assertEquals("Zu viele Zeichen. Maximal 7 Zeichen erlaubt.", exception.getMessage());
    }

    @Test
    void acceptsAnyLengthWithoutPositiveMaximum() {
        var withoutLimit = new RichTextInputElement();
        var withZeroLimit = new RichTextInputElement()
                .setMaxCharacters(0);
        var longMarkdown = "a".repeat(100_000);

        assertDoesNotThrow(() -> withoutLimit.validate(longMarkdown));
        assertDoesNotThrow(() -> withZeroLimit.validate(longMarkdown));
    }

    @Test
    void stillRequiresAValueWhenAMaximumIsSet() {
        var element = new RichTextInputElement()
                .setMaxCharacters(10);
        element.setRequired(true);

        assertThrows(RequiredValidationException.class, () -> element.performValidation(null));
        assertDoesNotThrow(() -> new RichTextInputElement().setMaxCharacters(10).performValidation(null));
    }

    @Test
    void includesMaximumLengthInEqualityAndHashCode() {
        var reference = createElement(500);
        var same = createElement(500);
        var different = createElement(501);

        assertEquals(reference, same);
        assertEquals(reference.hashCode(), same.hashCode());
        assertNotEquals(reference, different);
        assertNotEquals(reference.hashCode(), different.hashCode());
    }

    private static RichTextInputElement createElement(int maxCharacters) {
        var element = new RichTextInputElement()
                .setMaxCharacters(maxCharacters);
        element.setId("description");
        return element;
    }
}
