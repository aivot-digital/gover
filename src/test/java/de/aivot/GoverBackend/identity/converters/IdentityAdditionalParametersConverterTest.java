package de.aivot.GoverBackend.identity.converters;

import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentityAdditionalParametersConverterTest {

    @Test
    void convertToDatabaseColumn() {
        var converter = new IdentityAdditionalParametersConverter();

        var parameters = List.of(
                new IdentityAdditionalParameter()
                        .setKey("param1")
                        .setValue("value1"),
                new IdentityAdditionalParameter()
                        .setKey("param2")
                        .setValue("value2")
        );

        var json = converter
                .convertToDatabaseColumn(parameters);

        assertNotNull(json);
        assertTrue(json.contains("\"key\":\"param1\""));
        assertTrue(json.contains("\"value\":\"value1\""));
        assertTrue(json.contains("\"key\":\"param2\""));
        assertTrue(json.contains("\"value\":\"value2\""));

        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute() {
        var converter = new IdentityAdditionalParametersConverter();
        String json = """
                [
                    {
                        "key": "param1",
                        "value": "value1"
                    },
                    {
                        "key": "param2",
                        "value": "value2"
                    }
                ]
        """;

        var parameters = converter.convertToEntityAttribute(json);

        assertNotNull(parameters);
        assertEquals(2, parameters.size());

        var param1 = parameters.get(0);
        assertEquals("param1", param1.getKey());
        assertEquals("value1", param1.getValue());

        var param2 = parameters.get(1);
        assertEquals("param2", param2.getKey());
        assertEquals("value2", param2.getValue());

        assertNull(converter.convertToEntityAttribute(null));
    }
}