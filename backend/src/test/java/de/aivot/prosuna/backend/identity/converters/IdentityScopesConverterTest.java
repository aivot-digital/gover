package de.aivot.prosuna.backend.identity.converters;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityScopesConverterTest {

    @Test
    void convertToDatabaseColumn() {
        var converter = new IdentityScopesConverter(JsonMapperTestUtils.createMapper());
        var scopes = List.of("scope1", "scope2", "scope3");

        var json = converter.convertToDatabaseColumn(scopes);

        assertNotNull(json);
        assertTrue(json.contains("\"scope1\""));
        assertTrue(json.contains("\"scope2\""));
        assertTrue(json.contains("\"scope3\""));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute() {
        var converter = new IdentityScopesConverter(JsonMapperTestUtils.createMapper());
        String json = "[\"scope1\", \"scope2\", \"scope3\"]";

        var scopes = converter.convertToEntityAttribute(json);

        assertNotNull(scopes);
        assertEquals(3, scopes.size());
        assertEquals("scope1", scopes.get(0));
        assertEquals("scope2", scopes.get(1));
        assertEquals("scope3", scopes.get(2));

        assertNull(converter.convertToEntityAttribute(null));
    }
}
