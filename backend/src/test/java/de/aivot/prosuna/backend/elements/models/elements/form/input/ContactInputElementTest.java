package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.PhoneNumberValidation;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContactInputElementTest {
    @Test void normalizesPhoneNumbersAndValidatesTheSelectedMode() throws Exception {
        var field = new PhoneNumberInputElement();
        field.validate("+49 1234");
        assertEquals("+493012345678", field.formatValue("+49 30 123456-78"));
        assertEquals("+49 30 123456", field.toDisplayValue("+4930123456"));
        field.setValidationMode(PhoneNumberValidation.Strict);
        assertThrows(ValidationException.class, () -> field.validate("+49 1234"));
        assertThrows(ValidationException.class, () -> field.validate("+49 30 123456 ext. 7"));
        assertEquals("+49 30 123456 ext. 7", field.formatValue("+49 30 123456 ext. 7"));
    }
    @Test void normalizesIdnAndAppliesLocalDomainPolicy() throws Exception {
        var field = new EmailInputElement();
        assertEquals("Person@xn--bro-hoa.de", field.formatValue(" Person@büro.de "));
        assertThrows(ValidationException.class, () -> field.validate("person@verwaltung"));
        field.setAllowLocalDomains(true);
        field.validate("person@verwaltung");
        assertThrows(ValidationException.class, () -> field.validate("müller@example.org"));
        assertEquals("invalid", field.formatValue("invalid"));
    }
    @Test void handlesEmptyAndNonStringValuesWithoutSilentlyDiscardingThem() throws Exception {
        for (var field : new ContactInputElement[]{new PhoneNumberInputElement(), new EmailInputElement()}) {
            field.validate(null);
            field.validate(" ");
            field.validate("\u00a0\u202f");
            assertNull(field.formatValue(null));
            assertThrows(ValidationException.class, () -> field.validate(123));
            field.setRequired(true);
            assertThrows(ValidationException.class, () -> field.validate(null));
            assertThrows(ValidationException.class, () -> field.validate(" "));
            assertThrows(ValidationException.class, () -> field.validate("\u00a0\u202f"));
        }
    }
    @Test void doesNotTrimControlCharactersBeforeEmailValidation() {
        var field = new EmailInputElement();
        for (var value : new String[]{"person@example.org\r\n", "\u0000person@example.org", "\tperson@example.org"}) {
            assertEquals(value, field.formatValue(value));
            assertThrows(ValidationException.class, () -> field.validate(value));
        }
    }
    @Test void roundTripsDefinitionsAndRegistersFactories() throws Exception {
        var mapper = JsonMapperFactory.getInstance();
        for (var type : new ElementType[]{ElementType.PhoneNumber, ElementType.Email}) {
            var field = ElementType.getElementClass(type);
            var restored = mapper.readValue(mapper.writeValueAsString(field), BaseElement.class);
            assertEquals(field.getClass(), restored.getClass());
            assertEquals(type, restored.getType());
        }
        var email = new EmailInputElement().setAllowLocalDomains(true);
        assertTrue(mapper.readValue(mapper.writeValueAsString(email), EmailInputElement.class).getAllowLocalDomains());
    }
}
