package de.aivot.GoverBackend.models.elements.form.layout;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupLayoutTest extends AbstractElementTest<GroupLayout> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>(){{
            put("children", new LinkedList<>() {{
                add(new HashMap<>());
            }});
        }};
    }

    @Override
    protected GroupLayout newItem(Map<String, Object> json) {
        return new GroupLayout(json);
    }

    @Override
    protected void testAllFieldsFilled(GroupLayout item) {
        assertEquals(1, item.getChildren().size());
    }

    @Override
    protected void testAllFieldsNull(GroupLayout item) {
        assertNull(item.getChildren());
    }
}
