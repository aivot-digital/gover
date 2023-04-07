package de.aivot.GoverBackend.models.elements.form.content;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ImageTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "height": 100,
                    "width": 100,
                    "src": "src",
                    "alt": "alt"
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new Image(json);

        assertEquals(100, item.getHeight());
        assertEquals(100, item.getWidth());
        assertEquals("src", item.getSrc());
        assertEquals("alt", item.getAlt());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new Image(json);

        assertNull(item.getHeight());
        assertNull(item.getWidth());
        assertNull(item.getSrc());
        assertNull(item.getAlt());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new Image(json);
        });
    }

    @Test
    void isVisible() {
        var item = new Image(new HashMap<>());
        assertTrue(item.isVisible(new HashMap<>(), null));
    }

    @Test
    void isValid() {
        var item = new Image(new HashMap<>());
        assertTrue(item.isValid(new HashMap<>(), null));
    }

    @Test
    void toPdfRows() {
        var item = new Image(new HashMap<>());
        var rows = item.toPdfRows(new HashMap<>(), null);
        assertTrue(rows.isEmpty());
    }
}
