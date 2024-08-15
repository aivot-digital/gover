package de.aivot.GoverBackend.models.elements.form.content;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RichTextTest extends AbstractElementTest<RichText> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("content", "content");
        }};
    }

    @Override
    protected RichText newItem(Map<String, Object> json) {
        return new RichText(json);
    }

    @Override
    protected void testAllFieldsFilled(RichText item) {
        assertEquals("content", item.getContent());
    }

    @Override
    protected void testAllFieldsNull(RichText item) {
        assertEquals("", item.getContent());
    }
}
