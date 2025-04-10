package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.ScriptEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BaseElementDerivationContextTest {
    private BaseElementDerivationContext context;
    private HashMap<String, Object> inputValues;

    @BeforeEach
    public void setUp() {
        // Mock the JavascriptEngine and ScriptEngine
        var javascriptEngine = mock(JavascriptEngine.class);
        var legacyJavascriptEngine = mock(ScriptEngine.class);
        var noCodeEvaluationService = mock(NoCodeEvaluationService.class);

        // Create a global input values map to edit the input values during the tests
        inputValues = new HashMap<>();

        // Initialize the ElementDerivationContext with mocked engines and input values
        context = new BaseElementDerivationContext(javascriptEngine,
                legacyJavascriptEngine,
                noCodeEvaluationService,
                new RootElement(Map.of()),
                inputValues
        ) {};
    }

    @Test
    public void testSetAndGetVisibility() {
        // Test setting and getting visibility to false
        context.setVisibility("element1", false);
        assertFalse(context.isVisible("element1"));
        assertTrue(context.isInvisible("element1"));

        // Test setting and getting visibility to true
        context.setVisibility("element1", true);
        assertTrue(context.isVisible("element1"));
        assertFalse(context.isInvisible("element1"));

        // Test setting visibility to null (should default to true)
        context.setVisibility("element1", null);
        assertTrue(context.isVisible("element1"));
        assertFalse(context.isInvisible("element1"));
    }

    @Test
    public void testSetAndGetValue() {
        // Test setting and getting a computed value
        context.setValue("element1", "value1");
        assertEquals(Optional.of("value1"), context.getValue("element1"));

        // Test getting a value from input values
        inputValues.put("element2", "inputValue2");
        assertEquals(Optional.of("inputValue2"), context.getValue("element2"));

        // Test getting a value that does not exist
        assertEquals(Optional.empty(), context.getValue("element3"));

        // Test input values taking priority over computed values
        inputValues.put("element3", "inputValue3");
        context.setValue("element3", "value3");
        assertEquals(Optional.of("inputValue3"), context.getValue("element3"));
    }

    @Test
    public void testGetValueWithClass() {
        // Test getting a value with the correct class type
        context.setValue("element1", 123);
        assertEquals(Optional.of(123), context.getValue("element1", Integer.class));

        // Test getting a value from input values with the correct class type
        inputValues.put("element2", "inputValue2");
        assertEquals(Optional.of("inputValue2"), context.getValue("element2", String.class));

        // Test getting a value that does not exist with a class type
        assertEquals(Optional.empty(), context.getValue("element3", String.class));

        // Test getting a value with an incorrect class type (should throw ClassCastException)
        assertThrows(ClassCastException.class, () -> context.getValue("element1", String.class));
    }

    @Test
    public void testSetAndGetError() {
        // Test setting and getting an error
        context.setError("element1", "error1");
        assertEquals(Optional.of("error1"), context.getError("element1"));

        // Test setting an error to null (should remove the error)
        context.setError("element1", null);
        assertEquals(Optional.empty(), context.getError("element1"));
    }

    @Test
    public void testSetAndGetOverride() {
        // Test setting and getting an override
        BaseElement element = mock(BaseElement.class);
        context.setOverride("element1", element);
        assertEquals(Optional.of(element), context.getOverride("element1"));

        // Test setting an override to null (should remove the override)
        context.setOverride("element1", null);
        assertEquals(Optional.empty(), context.getOverride("element1"));
    }

    @Test
    public void testGetCombinedValues() {
        // Test getting combined values (input values should take priority over computed values)
        context.setValue("element1", "computedValue1");
        context.setValue("element2", "computedValue2");
        inputValues.put("element2", "inputValue2");
        inputValues.put("element3", "inputValue3");

        Map<String, Object> combinedValues = context.getCombinedValues();
        assertEquals("computedValue1", combinedValues.get("element1"));
        assertEquals("inputValue2", combinedValues.get("element2"));
        assertEquals("inputValue3", combinedValues.get("element3"));
    }

    @Test
    public void testHasErrors() {
        // Test checking for errors when there are none
        assertFalse(context.hasErrors());

        // Test checking for errors when there is an error
        context.setError("element1", "error1");
        assertTrue(context.hasErrors());

        // Test checking for errors after removing the error
        context.setError("element1", null);
        assertFalse(context.hasErrors());
    }
}