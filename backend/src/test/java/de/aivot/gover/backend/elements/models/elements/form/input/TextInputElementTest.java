package de.aivot.gover.backend.elements.models.elements.form.input;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TextInputElementTest {
    @Test
    void shouldFormatDateValueAsText() {
        var element = new TextInputElement();
        var date = ZonedDateTime.of(2028, 8, 7, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));

        assertEquals("07.08.2028", element.formatValue(date));
    }

    @Test
    void shouldIncludePrefixCopyableAndCopyValueTemplateInEqualityAndHashCode() {
        var reference = createElement("text_field", "Vorwahl", true, "https://example.test/{value}/");
        var same = createElement("text_field", "Vorwahl", true, "https://example.test/{value}/");
        var differentPrefix = createElement("text_field", "Kuerzel", true, "https://example.test/{value}/");
        var differentCopyable = createElement("text_field", "Vorwahl", false, "https://example.test/{value}/");
        var differentCopyValueTemplate = createElement("text_field", "Vorwahl", true, "https://other.test/{value}/");

        assertEquals(reference, same);
        assertEquals(reference.hashCode(), same.hashCode());
        assertNotEquals(reference, differentPrefix);
        assertNotEquals(reference.hashCode(), differentPrefix.hashCode());
        assertNotEquals(reference, differentCopyable);
        assertNotEquals(reference.hashCode(), differentCopyable.hashCode());
        assertNotEquals(reference, differentCopyValueTemplate);
        assertNotEquals(reference.hashCode(), differentCopyValueTemplate.hashCode());
    }

    private static TextInputElement createElement(String id, String prefix, boolean copyable, String copyValueTemplate) {
        var element = new TextInputElement();
        element.setId(id);
        element.setPrefix(prefix);
        element.setCopyable(copyable);
        element.setCopyValueTemplate(copyValueTemplate);
        return element;
    }
}
