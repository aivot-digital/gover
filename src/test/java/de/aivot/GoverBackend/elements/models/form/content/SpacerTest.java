package de.aivot.GoverBackend.elements.models.form.content;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpacerTest extends AbstractElementTest<Spacer> {

    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("height", 100);
        }};
    }

    @Override
    protected Spacer newItem(Map<String, Object> json) {
        return new Spacer(json);
    }

    @Override
    protected void testAllFieldsFilled(Spacer item) {
        assertEquals("100", item.getHeight());
    }

    @Override
    protected void testAllFieldsNull(Spacer item) {
        assertEquals("100", item.getHeight());
    }
}
