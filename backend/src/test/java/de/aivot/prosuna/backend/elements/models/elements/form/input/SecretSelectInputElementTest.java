package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretSelectInputElementTest {
    private final SecretSelectInputElement element = new SecretSelectInputElement();

    @Test
    void shouldPersistTrimmedSecretKeys() {
        assertEquals("secret-key", element.formatValue("  secret-key  "));
        assertNull(element.formatValue("   "));
        assertNull(element.formatValue(null));
    }

    @Test
    void shouldValidateRequiredValuesWithoutRequiringEmbeddedOptions() {
        assertDoesNotThrow(() -> element.validate("secret-key"));

        element.setRequired(true);
        assertThrows(ValidationException.class, () -> element.validate(" "));
    }

    @Test
    void shouldRenderAndEvaluateSecretKeys() {
        assertEquals("secret-key", element.toDisplayValue("secret-key"));
        assertEquals("Keine Angabe", element.toDisplayValue(null));
        assertTrue(element.evaluate(ConditionOperator.Equals, " secret-key ", "secret-key"));
        assertFalse(element.evaluate(ConditionOperator.NotEquals, "secret-key", "secret-key"));
        assertTrue(element.evaluate(ConditionOperator.Empty, " ", null));
        assertTrue(element.evaluate(ConditionOperator.NotEmpty, "secret-key", null));
        assertFalse(element.evaluate(ConditionOperator.Includes, "secret-key", "secret"));
    }

    @Test
    void shouldRoundTripThroughBaseElementSerialization() throws Exception {
        element.setId("secret");
        element.setPlaceholder("Geheimnis auswählen");

        var serialized = JsonMapperFactory.getInstance().writeValueAsString(element);
        var deserialized = JsonMapperFactory.getInstance().readValue(serialized, BaseElement.class);

        var secretSelect = assertInstanceOf(SecretSelectInputElement.class, deserialized);
        assertEquals(ElementType.SecretSelectInput, secretSelect.getType());
        assertEquals("Geheimnis auswählen", secretSelect.getPlaceholder());
    }
}
