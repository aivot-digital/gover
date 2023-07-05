package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.services.ScriptService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FunctionCodeTest {
    @Test
    void constructor() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("code", "function main($data, $element, $id) { return toUpperCase($id); }\nfunction toUpperCase(str) { return str.toUpperCase(); }");
        }});
        assertNotNull(fn.getCode());
        assertFalse(fn.getCode().isEmpty());
    }

    /*
    @Test
    void evaluateToString() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("functions", new HashMap<String, String>() {{
                put("main", "($data, $element, $id) { return toUpperCase($id); }");
                put("toUpperCase", "(str) { return str.toUpperCase(); }");
            }});
            put("mainFunction", "main");
        }}) {
        };
        FunctionResult evalResult = fn.evaluate(new BaseElement(new HashMap<>()) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        }, new HashMap<>(), "id", ScriptService.getEngine());

        assertNotNull(evalResult.getStringValue());
        assertEquals("ID", evalResult.getStringValue());
    }

    @Test
    void evaluateToBoolean() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("functions", new HashMap<String, String>() {{
                put("main", "($data, $element, $id) { return true; }");
            }});
            put("mainFunction", "main");
        }}) {
        };
        FunctionResult evalResult = fn.evaluate(new BaseElement(new HashMap<>()) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        }, new HashMap<>(), "id", ScriptService.getEngine());

        assertNotNull(evalResult.getBooleanValue());
        assertEquals(true, evalResult.getBooleanValue());
    }


    @Test
    void evaluateToInteger() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("functions", new HashMap<String, String>() {{
                put("main", "($data, $element, $id) { return 42; }");
            }});
            put("mainFunction", "main");
        }}) {
        };
        FunctionResult evalResult = fn.evaluate(new BaseElement(new HashMap<>()) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        }, new HashMap<>(), "id", ScriptService.getEngine());

        assertNotNull(evalResult.getIntegerValue());
        assertEquals(42, evalResult.getIntegerValue());
    }

    @Test
    void evaluateIntegerValueToDouble() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("functions", new HashMap<String, String>() {{
                put("main", "($data, $element, $id) { return 42.0; }");
            }});
            put("mainFunction", "main");
        }}) {
        };
        FunctionResult evalResult = fn.evaluate(new BaseElement(new HashMap<>()) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        }, new HashMap<>(), "id", ScriptService.getEngine());

        assertNotNull(evalResult.getDoubleValue());
        assertEquals(42, evalResult.getDoubleValue());
    }

    @Test
    void evaluateToDouble() {
        var fn = new FunctionCode(new HashMap<>() {{
            put("functions", new HashMap<String, String>() {{
                put("main", "($data, $element, $id) { return 42.1; }");
            }});
            put("mainFunction", "main");
        }}) {
        };
        FunctionResult evalResult = fn.evaluate(new BaseElement(new HashMap<>()) {
            @Override
            public void applyValues(Map<String, Object> values) {

            }
        }, new HashMap<>(), "id", ScriptService.getEngine());

        assertNotNull(evalResult.getDoubleValue());
        assertEquals(42.1, evalResult.getDoubleValue());
    }
     */
}
