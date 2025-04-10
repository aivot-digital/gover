package de.aivot.GoverBackend.elements.models.form;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;

import java.util.*;

class InputElementTest extends AbstractElementTest<BaseInputElement<Object>> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("label", "label");
            put("hint", "hint");
            put("required", true);
            put("disabled", false);
            put("isValid", new HashMap<>() {{
                put("requirements", "requirements isValid");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('isValid')");
                }});
            }});
            put("isDisabled", new HashMap<>() {{
                put("requirements", "requirements isDisabled");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('isDisabled')");
                }});
            }});
            put("isRequired", new HashMap<>() {{
                put("requirements", "requirements isRequired");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('isRequired')");
                }});
            }});
            put("computeValue", new HashMap<>() {{
                put("requirements", "requirements computeValue");
                put("mainFunction", "main");
                put("functions", new HashMap<>() {{
                    put("main", "alert('computeValue')");
                }});
            }});
        }};
    }

    @Override
    protected BaseInputElement<Object> newItem(Map<String, Object> json) {
        //return new InputElement<>(json) {
        //    @Override
        //    public boolean isValid(Object value, String idPrefix) {
        //        return false;
        //    }
//
        //    @Override
        //    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, Object value, String idPrefix, ScriptEngine scriptEngine) {
        //        return null;
        //    }
        //};
        return null;
    }

    @Override
    protected void testAllFieldsFilled(BaseInputElement<Object> item) {
        /*
        assertEquals("label", item.getLabel());
        assertEquals("hint", item.getHint());
        assertEquals(true, item.getRequired());
        assertEquals(false, item.getDisabled());

        assertNotNull(item.getValidate());
        //assertNotNull(item.getIsDisabled());
        //assertNotNull(item.getIsRequired());
        assertNotNull(item.getComputeValue());

        // TODO: Check details of functions
         */
    }

    @Override
    protected void testAllFieldsNull(BaseInputElement<Object> item) {
        /*
        assertNull(item.getLabel());
        assertNull(item.getHint());
        assertNull(item.getRequired());
        assertNull(item.getDisabled());

        assertNull(item.getValidate());
        //assertNull(item.getIsDisabled());
        //assertNull(item.getIsRequired());
        assertNull(item.getComputeValue());
         */
    }
}
