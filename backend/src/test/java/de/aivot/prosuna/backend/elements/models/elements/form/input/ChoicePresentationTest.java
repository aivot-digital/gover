package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.MultiChoicePresentation;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChoicePresentationTest {
    @ParameterizedTest
    @EnumSource(SelectInputPresentation.class)
    void preservesSingleChoiceValuesAndValidationAcrossPresentations(SelectInputPresentation presentation) throws Exception {
        var original = new SelectInputElement().setPresentation(presentation).setDisplayInline(true)
                .setOptions(List.of(SelectInputElementOption.of("BE", "Berlin")));
        var mapper = JsonMapperFactory.getInstance();
        var restored = assertInstanceOf(SelectInputElement.class,
                mapper.readValue(mapper.writeValueAsString(original), BaseElement.class));
        assertEquals(presentation, restored.getPresentation());
        assertEquals(true, restored.getDisplayInline());
        assertEquals("Berlin", restored.toDisplayValue("BE"));
        assertDoesNotThrow(() -> restored.validate("BE"));
        assertThrows(ValidationException.class, () -> restored.validate("Berlin"));
    }

    @ParameterizedTest
    @EnumSource(MultiChoicePresentation.class)
    void preservesMultipleChoiceValuesAndMinimumAcrossPresentations(MultiChoicePresentation presentation) throws Exception {
        var original = new MultiCheckboxInputElement().setPresentation(presentation)
                .setOptions(List.of(new MultiCheckboxInputElementOption().setValue("BE").setLabel("Berlin"),
                        new MultiCheckboxInputElementOption().setValue("HH").setLabel("Hamburg")))
                .setMinimumRequiredOptions(2);
        original.setRequired(true);
        var mapper = JsonMapperFactory.getInstance();
        var restored = assertInstanceOf(MultiCheckboxInputElement.class,
                mapper.readValue(mapper.writeValueAsString(original), BaseElement.class));
        assertEquals(presentation, restored.getPresentation());
        assertDoesNotThrow(() -> restored.validate(List.of("BE", "HH")));
        assertThrows(ValidationException.class, () -> restored.validate(List.of("BE")));
        assertThrows(ValidationException.class, () -> restored.validate(List.of("BE", "unknown")));
    }

    @Test
    void readsLegacyRadioAsSelectAndWritesTheCanonicalType() throws Exception {
        var mapper = JsonMapperFactory.getInstance();
        var restored = assertInstanceOf(SelectInputElement.class, mapper.readValue(
                """
                {"type":11,"id":"legacy","displayInline":true,"toggleButtons":true}
                """, BaseElement.class));
        assertEquals(SelectInputPresentation.Radio, restored.getPresentation());
        assertEquals(12, restored.getType().getKey());
        assertEquals(12, mapper.readTree(mapper.writeValueAsString(restored)).path("type").asInt());
        assertEquals(true, restored.getDisplayInline());
        assertEquals(true, restored.getToggleButtons());
        assertEquals(SelectInputPresentation.Dropdown, new SelectInputElement().getPresentation());
        assertNull(new MultiCheckboxInputElement().getPresentation());
    }
}
