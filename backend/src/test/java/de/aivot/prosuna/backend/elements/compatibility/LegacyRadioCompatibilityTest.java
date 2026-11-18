package de.aivot.prosuna.backend.elements.compatibility;

import de.aivot.prosuna.backend.core.converters.GroupLayoutConverter;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyRadioCompatibilityTest {
    private static final String LEGACY_RADIO = """
            {"id":"contact","type":11,"destinationKey":"contactMethod","label":"Kontaktweg",
             "displayInline":true,"toggleButtons":true,"optionsSource":"manual",
             "options":[{"value":"email","label":"E-Mail"},{"value":"post","label":"Post"}]}
            """;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":11,\"id\":\"legacy\"}",
            "{\"id\":\"legacy\",\"presentation\":null,\"type\":11}",
            "{\"type\":11,\"presentation\":\"dropdown\",\"id\":\"legacy\"}",
            "{\"id\":\"legacy\",\"type\":\"11\"}"
    })
    void alwaysNormalizesLegacyTypeRegardlessOfPropertyOrder(String json) {
        var mapper = JsonMapperFactory.getInstance();
        var element = assertInstanceOf(SelectInputElement.class, mapper.readValue(json, BaseElement.class));
        assertEquals(ElementType.Select, element.getType());
        assertEquals(SelectInputPresentation.Radio, element.getPresentation());
        var stored = mapper.readTree(mapper.writeValueAsString(element));
        assertEquals(12, stored.path("type").asInt());
        assertEquals("radio", stored.path("presentation").asText());
    }

    @Test
    void convertsNestedPersistedElementsWithoutChangingTheirValuesOrIdentity() {
        var converter = new GroupLayoutConverter();
        var json = "{\"type\":3,\"id\":\"root\",\"children\":[{\"type\":9,\"id\":\"rows\",\"children\":[" + LEGACY_RADIO + "]}]}";
        var root = converter.convertToEntityAttribute(json);
        var field = root.findChild("contact", SelectInputElement.class).orElseThrow();
        assertEquals(SelectInputPresentation.Radio, field.getPresentation());
        assertEquals("contactMethod", field.getDestinationKey());
        assertEquals(true, field.getDisplayInline());
        assertEquals(true, field.getToggleButtons());
        assertEquals("E-Mail", field.toDisplayValue("email"));
        assertDoesNotThrow(() -> field.validate("email"));
        var stored = JsonMapperFactory.getInstance().readTree(converter.convertToDatabaseColumn(root));
        assertEquals(12, stored.path("children").get(0).path("children").get(0).path("type").asInt());
        var reopened = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(root));
        assertEquals(stored, JsonMapperFactory.getInstance().readTree(converter.convertToDatabaseColumn(reopened)));
    }

    @Test
    void convertsUntypedConfigurationOnlyWhenItIsReadAsAnElementDefinition() {
        var mapper = JsonMapperFactory.getInstance();
        var values = mapper.readValue("{\"layout\":{\"type\":\"Literal\",\"value\":" + LEGACY_RADIO + "}}", AuthoredElementValues.class);
        var raw = assertInstanceOf(Map.class, values.getLiteral("layout"));
        assertEquals(11, raw.get("type"));
        var element = assertInstanceOf(SelectInputElement.class, new UiDefinitionInputElement().formatValue(raw));
        assertEquals(SelectInputPresentation.Radio, element.getPresentation());
        assertEquals(11, raw.get("type"));
        values.putLiteral("layout", element);
        assertEquals(12, mapper.readTree(mapper.writeValueAsString(values)).path("layout").path("value").path("type").asInt());
    }

    @Test
    void keepsExistingSelectDefaultsAndAcceptsLegacyRootConstraints() {
        var mapper = JsonMapperFactory.getInstance();
        var select = mapper.readValue("{\"type\":12,\"id\":\"existing\"}", SelectInputElement.class);
        assertEquals(SelectInputPresentation.Dropdown, select.getPresentation());
        var definition = mapper.readValue("{\"type\":29,\"id\":\"layout\",\"elementType\":11}", UiDefinitionInputElement.class);
        assertEquals(ElementType.Select, definition.getElementType());
        assertDoesNotThrow(() -> definition.performValidation(mapper.readValue(LEGACY_RADIO, BaseElement.class)));
    }
}
