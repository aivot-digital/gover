package de.aivot.GoverBackend.models.elements.form.content;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class RichTextTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "content": "content"
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new RichText(json);

        assertEquals("content", item.getContent());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new RichText(json);

        assertNull(item.getContent());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new RichText(json);
        });
    }

    @Test
    void isVisible() {
        var item = new RichText(new HashMap<>());
        assertTrue(item.isVisible(new HashMap<>(), null));
    }

    @Test
    void isValid() {
        var item = new RichText(new HashMap<>());
        assertTrue(item.isValid(new HashMap<>(), null));
    }

    @Test
    void toPdfRows() {
        var item = new RichText(new HashMap<>());
        var rows = item.toPdfRows(new HashMap<>(), null);
        assertTrue(rows.isEmpty());
    }
}
