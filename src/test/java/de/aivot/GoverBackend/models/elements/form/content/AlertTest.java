package de.aivot.GoverBackend.models.elements.form.content;


import de.aivot.GoverBackend.enums.AlertType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "alertType": 0,
                    "title": "title",
                    "text": "text"
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new Alert(json);

        assertEquals(AlertType.Error, item.getAlertType());
        assertEquals("title", item.getTitle());
        assertEquals("text", item.getText());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new Alert(json);

        assertNull(item.getAlertType());
        assertNull(item.getTitle());
        assertNull(item.getText());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new Alert(json);
        });
    }

    @Test
    void isVisible() {
        var item = new Alert(new HashMap<>());
        assertTrue(item.isVisible(new HashMap<>(), null));
    }

    @Test
    void isValid() {
        var item = new Alert(new HashMap<>());
        assertTrue(item.isValid(new HashMap<>(), null));
    }

    @Test
    void toPdfRows() {
        var item = new Alert(new HashMap<>());
        var rows = item.toPdfRows(new HashMap<>(), null);
        assertTrue(rows.isEmpty());
    }
}
