package de.aivot.GoverBackend.elements.models.form.content;


import de.aivot.GoverBackend.enums.AlertType;
import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest extends AbstractElementTest<Alert> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("title", "title");
            put("text", "text");
            put("alertType", AlertType.Error.getKey());
        }};
    }

    @Override
    protected Alert newItem(Map<String, Object> json) {
        return new Alert(json);
    }

    @Override
    protected void testAllFieldsFilled(Alert item) {
        assertEquals("title", item.getTitle());
        assertEquals("text", item.getText());
        assertEquals(AlertType.Error, item.getAlertType());
    }

    @Override
    protected void testAllFieldsNull(Alert item) {
        assertEquals("", item.getTitle());
        assertEquals("", item.getText());
        assertEquals(AlertType.Info, item.getAlertType());
    }
}
