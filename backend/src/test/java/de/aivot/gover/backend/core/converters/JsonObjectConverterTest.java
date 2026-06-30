package de.aivot.gover.backend.core.converters;

import de.aivot.gover.backend.core.converters.JsonObjectConverter;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonObjectConverterTest {
    @Test
    void convertToDatabaseColumn_PreservesExplicitNullMapEntries() {
        var nested = new LinkedHashMap<String, Object>();
        nested.put("innerClearedField", null);
        nested.put("innerValue", "value");

        var payload = new LinkedHashMap<String, Object>();
        payload.put("clearedField", null);
        payload.put("nested", nested);

        var converter = new JsonObjectConverter();

        var serialized = converter.convertToDatabaseColumn(payload);
        var roundTripped = converter.convertToEntityAttribute(serialized);

        assertTrue(serialized.contains("\"clearedField\":null"));
        assertTrue(serialized.contains("\"innerClearedField\":null"));
        assertTrue(roundTripped.containsKey("clearedField"));
        assertNull(roundTripped.get("clearedField"));

        var roundTrippedNested = (Map<?, ?>) roundTripped.get("nested");
        assertTrue(roundTrippedNested.containsKey("innerClearedField"));
        assertNull(roundTrippedNested.get("innerClearedField"));
        assertEquals("value", roundTrippedNested.get("innerValue"));
    }
}
