package de.aivot.GoverBackend.models.elements;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.functions.FunctionCode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BaseElementTest extends AbstractElementTest<BaseElement> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("type", ElementType.Root.getKey());
            put("id", "id");
            put("name", "name");
            put("testProtocolSet", new HashMap<>() {{
                put("technicalTest", new HashMap<>() {{
                    put("userId", 0);
                    put("timestamp", "2023-04-07");
                }});
                put("professionalTest", new HashMap<>() {{
                    put("userId", 1);
                    put("timestamp", "2023-04-08");
                }});
            }});
            put("isVisible", new HashMap<>() {{
                put("requirements", "requirements isVisible");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('isVisible')");
                }});
            }});
            put("patchElement", new HashMap<>() {{
                put("requirements", "requirements patchElement");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('patchElement')");
                }});
            }});
        }};
    }

    @Override
    protected BaseElement newItem(Map<String, Object> json) {
        return new BaseElement(json) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        };
    }

    @Override
    protected void testAllFieldsFilled(BaseElement item) {
        assertEquals(ElementType.Root, item.getType());
        assertEquals("id", item.getId());
        assertEquals("name", item.getName());

        assertNotNull(item.getTestProtocolSet());

        assertNotNull(item.getTestProtocolSet().getTechnicalTest());
        assertEquals(0, item.getTestProtocolSet().getTechnicalTest().getUserId());
        assertEquals("2023-04-07", item.getTestProtocolSet().getTechnicalTest().getTimestamp());

        assertNotNull(item.getTestProtocolSet().getProfessionalTest());
        assertEquals(1, item.getTestProtocolSet().getProfessionalTest().getUserId());
        assertEquals("2023-04-08", item.getTestProtocolSet().getProfessionalTest().getTimestamp());

        assertNotNull(item.getIsVisible());
        assertEquals("requirements isVisible", item.getIsVisible().getRequirements());
        assertInstanceOf(FunctionCode.class, item.getIsVisible());
        assertEquals("main", ((FunctionCode) item.getIsVisible()).getMainFunction());
        assertEquals(1, ((FunctionCode) item.getIsVisible()).getFunctions().size());

        assertNotNull(item);
        assertNotNull(item.getPatchElement());
        assertEquals("requirements patchElement", item.getPatchElement().getRequirements());
        assertEquals("main", item.getPatchElement().getMainFunction());
        assertEquals(1, item.getPatchElement().getFunctions().size());
    }

    @Override
    protected void testAllFieldsNull(BaseElement item) {
        assertNull(item.getId());
        assertNull(item.getType());
        assertNull(item.getName());
        assertNull(item.getTestProtocolSet());
        assertNull(item.getIsVisible());
        assertNull(item.getPatchElement());
    }
}
