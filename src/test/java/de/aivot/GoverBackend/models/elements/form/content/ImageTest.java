package de.aivot.GoverBackend.models.elements.form.content;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageTest extends AbstractElementTest<Image> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("height", 100);
            put("width", 100);
            put("src", "src");
            put("alt", "alt");
        }};
    }

    @Override
    protected Image newItem(Map<String, Object> json) {
        return new Image(json);
    }

    @Override
    protected void testAllFieldsFilled(Image item) {
        assertEquals(100, item.getHeight());
        assertEquals(100, item.getWidth());
        assertEquals("src", item.getSrc());
        assertEquals("alt", item.getAlt());
    }

    @Override
    protected void testAllFieldsNull(Image item) {
        assertEquals(100, item.getHeight());
        assertEquals(100, item.getWidth());
        assertEquals("", item.getSrc());
        assertEquals("", item.getAlt());
    }
}
