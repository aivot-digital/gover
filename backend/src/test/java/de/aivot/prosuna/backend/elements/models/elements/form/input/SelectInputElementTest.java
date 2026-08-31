package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SelectInputElementTest {
    @Test
    void shouldUseDropdownAsDefaultPresentation() {
        assertEquals(SelectInputPresentation.Dropdown, new SelectInputElement().getPresentation());
    }

    @Test
    void shouldFormatAuthoredValuesAsStrings() {
        assertEquals("42", new SelectInputElement().formatValue(42));
    }

    @Test
    void shouldRoundTripComboboxPresentationThroughBaseElementSerialization() throws Exception {
        var element = new SelectInputElement()
                .setPresentation(SelectInputPresentation.Combobox);

        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(element);

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        var select = assertInstanceOf(SelectInputElement.class, deserialized);
        assertEquals(SelectInputPresentation.Combobox, select.getPresentation());
    }

    @Test
    void shouldNormalizeUnknownPresentationToDropdown() throws Exception {
        var objectMapper = ObjectMapperFactory.getInstance();
        var serialized = objectMapper
                .writeValueAsString(new SelectInputElement())
                .replace("\"presentation\":\"dropdown\"", "\"presentation\":\"unknown\"");
        var deserialized = objectMapper.readValue(serialized, BaseElement.class);

        var select = assertInstanceOf(SelectInputElement.class, deserialized);
        assertEquals(SelectInputPresentation.Dropdown, select.getPresentation());
    }
}
