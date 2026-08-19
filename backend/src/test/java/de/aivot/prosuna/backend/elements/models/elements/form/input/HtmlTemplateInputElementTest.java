package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.HtmlTemplateInputElementValue;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTemplateInputElementTest {
    @Test
    void validateShouldRequireAssetKeyWhenRequired() {
        var element = new HtmlTemplateInputElement();
        element.setRequired(true);

        var emptyValue = new HtmlTemplateInputElementValue()
                .setAssetKey(" ")
                .setSlots(Map.of());

        assertThrows(RequiredValidationException.class, () -> element.performValidation(emptyValue));
    }

    @Test
    void validateShouldAcceptValueWithAssetKey() {
        var element = new HtmlTemplateInputElement();
        element.setRequired(true);

        var value = new HtmlTemplateInputElementValue()
                .setAssetKey("templateAssetKey")
                .setSlots(Map.of("header_left", "Header"));

        assertDoesNotThrow(() -> element.performValidation(value));
        assertEquals("1 Slot ausgefüllt", element.toDisplayValue(value));
    }

    @Test
    void evaluateShouldCheckWhetherAssetKeyIsFilled() {
        var element = new HtmlTemplateInputElement();
        var value = new HtmlTemplateInputElementValue()
                .setAssetKey("templateAssetKey")
                .setSlots(Map.of());

        assertTrue(element.evaluate(ConditionOperator.NotEmpty, value, null));
        assertFalse(element.evaluate(ConditionOperator.Empty, value, null));
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        var serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsString(new HtmlTemplateInputElement());

        var deserialized = ObjectMapperFactory
                .getInstance()
                .readValue(serialized, BaseElement.class);

        assertInstanceOf(HtmlTemplateInputElement.class, deserialized);
    }
}
