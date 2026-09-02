package de.aivot.prosuna.backend.identity.converters;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityAttributesConverterTest {

    @Test
    void convertToDatabaseColumn() {
        var converter = new IdentityAttributesConverter(JsonMapperTestUtils.createMapper());
        var attributes = List.of(
                new IdentityAttributeMapping()
                        .setLabel("Email")
                        .setDescription("The user's email address")
                        .setKeyInData("email"),
                new IdentityAttributeMapping()
                        .setLabel("Name")
                        .setDescription("The user's full name")
                        .setKeyInData("name")
        );

        var json = converter.convertToDatabaseColumn(attributes);

        assertNotNull(json);
        assertTrue(json.contains("\"label\":\"Email\""));
        assertTrue(json.contains("\"description\":\"The user's email address\""));
        assertTrue(json.contains("\"keyInData\":\"email\""));
        assertTrue(json.contains("\"label\":\"Name\""));
        assertTrue(json.contains("\"description\":\"The user's full name\""));
        assertTrue(json.contains("\"keyInData\":\"name\""));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute() {
        var converter = new IdentityAttributesConverter(JsonMapperTestUtils.createMapper());
        String json = """
                [
                    {
                        "label": "Email",
                        "description": "The user's email address",
                        "keyInData": "email"
                    },
                    {
                        "label": "Name",
                        "description": "The user's full name",
                        "keyInData": "name"
                    }
                ]
        """;

        var attributes = converter.convertToEntityAttribute(json);

        assertNotNull(attributes);
        assertEquals(2, attributes.size());

        var attr1 = attributes.get(0);
        assertEquals("Email", attr1.getLabel());
        assertEquals("The user's email address", attr1.getDescription());
        assertEquals("email", attr1.getKeyInData());

        var attr2 = attributes.get(1);
        assertEquals("Name", attr2.getLabel());
        assertEquals("The user's full name", attr2.getDescription());
        assertEquals("name", attr2.getKeyInData());

        assertNull(converter.convertToEntityAttribute(null));
    }
}
