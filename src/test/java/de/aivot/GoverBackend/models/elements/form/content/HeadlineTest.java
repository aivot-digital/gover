package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class HeadlineTest {
    @Test
    void testSerializeSuccessful() throws JSONException {
        var jsonStr = """
                {
                    "content": "content",
                    "small": true
                }
                    """;

        var json = new JSONObject(jsonStr).toMap();
        var item = new Headline(json);

        assertEquals("content", item.getContent());
        assertEquals(true, item.getSmall());
    }

    @Test
    void testSerializeEmpty() {
        var jsonStr = "{}";

        var json = new JSONObject(jsonStr).toMap();
        var item = new Headline(json);

        assertNull(item.getContent());
        assertNull(item.getSmall());
    }

    @Test
    void testSerializeInvalidJson() {
        var jsonStr = "INVALID JSON";

        assertThrows(JSONException.class, () -> {
            var json = new JSONObject(jsonStr).toMap();
            new Headline(json);
        });
    }

    @Test
    void isVisible() {
        var item = new Headline(new HashMap<>());
        assertTrue(item.isVisible(new HashMap<>(), null));
    }

    @Test
    void isValid() {
        var item = new Headline(new HashMap<>());
        assertTrue(item.isValid(new HashMap<>(), null));
    }

    @Test
    void toPdfRows() {
        var item = new Headline(new HashMap<>() {{
            put("content", "content");
            put("small", true);
        }});

        var rows = item.toPdfRows(new HashMap<>(), null);
        assertEquals(1, rows.size());
        assertInstanceOf(HeadlinePdfRowDto.class, rows.get(0));
        assertEquals("content", ((HeadlinePdfRowDto) rows.get(0)).text);
        assertEquals(Headline.HEADLINE_SIZE_SMALL, ((HeadlinePdfRowDto) rows.get(0)).size);
    }
}
