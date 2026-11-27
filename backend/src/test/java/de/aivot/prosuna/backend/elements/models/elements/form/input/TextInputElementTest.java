package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TextInputElementTest {
    private final ZoneId originalZoneId = ApplicationTimeZone.getZoneId();

    @AfterEach
    void restoreApplicationTimeZone() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void shouldFormatExplicitDateValuesAsText() {
        var element = new TextInputElement();

        assertEquals("07.08.2028", element.formatValue(LocalDate.of(2028, 8, 7)));
        assertEquals("08.2028", element.formatValue(YearMonth.of(2028, 8)));
        assertEquals("2028", element.formatValue(Year.of(2028)));
    }

    @Test
    void shouldFormatLegacyZonedDateValueInApplicationTimeZone() {
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
        var element = new TextInputElement();
        var date = ZonedDateTime.of(2028, 8, 6, 22, 30, 0, 0, ZoneOffset.UTC);

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
