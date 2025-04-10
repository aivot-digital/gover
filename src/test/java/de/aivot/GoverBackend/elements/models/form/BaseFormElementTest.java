package de.aivot.GoverBackend.elements.models.form;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseFormElementTest extends AbstractElementTest<BaseFormElement> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("weight", 5);
        }};
    }

    @Override
    protected BaseFormElement newItem(Map<String, Object> json) {
        return new BaseFormElement(json) {
        };
    }

    @Override
    protected void testAllFieldsFilled(BaseFormElement item) {
        assertEquals(5, item.getWeight());
    }

    @Override
    protected void testAllFieldsNull(BaseFormElement item) {
        assertNull(item.getWeight());
    }
}
