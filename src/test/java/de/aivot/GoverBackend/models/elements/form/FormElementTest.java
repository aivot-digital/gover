package de.aivot.GoverBackend.models.elements.form;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormElementTest extends AbstractElementTest<FormElement> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("weight", 5);
        }};
    }

    @Override
    protected FormElement newItem(Map<String, Object> json) {
        return new FormElement(json) {
        };
    }

    @Override
    protected void testAllFieldsFilled(FormElement item) {
        assertEquals(5, item.getWeight());
    }

    @Override
    protected void testAllFieldsNull(FormElement item) {
        assertNull(item.getWeight());
    }
}
