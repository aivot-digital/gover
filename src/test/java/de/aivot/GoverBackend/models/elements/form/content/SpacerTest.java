package de.aivot.GoverBackend.models.elements.form.content;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SpacerTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "height": 100
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new Spacer(json);

        assertEquals(100, item.getHeight());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new Spacer(json);

        assertNull(item.getHeight());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new Spacer(json);
        });
    }

    @Test
    void isVisible() {
        var item = new Spacer(new HashMap<>());
        assertTrue(item.isVisible(new HashMap<>(), null));
    }

    @Test
    void isValid() {
        var item = new Spacer(new HashMap<>());
        assertTrue(item.isValid(new HashMap<>(), null));
    }

    @Test
    void toPdfRows() {
        var item = new Spacer(new HashMap<>());
        var rows = item.toPdfRows(new HashMap<>(), null);
        assertTrue(rows.isEmpty());
    }
}
