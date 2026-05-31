package de.aivot.GoverBackend.elements.models.elements.form.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TextInputElementTest {
    @Test
    void shouldIncludePrefixAndCopyableInEqualityAndHashCode() {
        var reference = createElement("text_field", "Vorwahl", true);
        var same = createElement("text_field", "Vorwahl", true);
        var differentPrefix = createElement("text_field", "Kuerzel", true);
        var differentCopyable = createElement("text_field", "Vorwahl", false);

        assertEquals(reference, same);
        assertEquals(reference.hashCode(), same.hashCode());
        assertNotEquals(reference, differentPrefix);
        assertNotEquals(reference.hashCode(), differentPrefix.hashCode());
        assertNotEquals(reference, differentCopyable);
        assertNotEquals(reference.hashCode(), differentCopyable.hashCode());
    }

    private static TextInputElement createElement(String id, String prefix, boolean copyable) {
        var element = new TextInputElement();
        element.setId(id);
        element.setPrefix(prefix);
        element.setCopyable(copyable);
        return element;
    }
}
