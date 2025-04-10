package de.aivot.GoverBackend.javascript.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavascriptCodeTest {

    @Test
    void readValue() {
        var json = """
                {
                    "code": "test"
                }
                """;

        var code = assertDoesNotThrow(() -> new ObjectMapper()
                .readValue(json, JavascriptCode.class));

        assertEquals("test", code.getCode());
    }

    @Test
    void writeValue() {
        var code = new JavascriptCode()
                .setCode("test");

        var json = assertDoesNotThrow(() -> new ObjectMapper()
                .writeValueAsString(code));

        assertEquals("{\"code\":\"test\"}", json);
    }

    @Test
    void getCode() {
        var code = new JavascriptCode()
                .setCode("test");
        assertEquals("test", code.getCode());
    }

    @Test
    void isEmpty() {
        var code = new JavascriptCode()
                .setCode("test");
        assertFalse(code.isEmpty());

        code = new JavascriptCode();
        assertTrue(code.isEmpty());

        code = new JavascriptCode()
                .setCode("");
        assertTrue(code.isEmpty());
    }

    @Test
    void testEquals() {
        var code1 = new JavascriptCode().setCode("test");
        var code2 = new JavascriptCode().setCode("test");
        var code3 = new JavascriptCode().setCode("test2");

        assertEquals(code1, code2);
        assertNotEquals(code1, code3);
    }

    @Test
    void testHashCode() {
        var code1 = new JavascriptCode().setCode("test");
        var code2 = new JavascriptCode().setCode("test");
        var code3 = new JavascriptCode().setCode("test2");

        assertEquals(code1.hashCode(), code2.hashCode());
        assertNotEquals(code1.hashCode(), code3.hashCode());
    }

    @Test
    void getReferencedIds() {
        var code = new JavascriptCode()
                .setCode("""
                        (function(){
                            //>>>text_123
                            const plz = ctx.inputValues.text_456;
                        
                            if (plz == null || plz.length !== 5) {
                                return null;
                            }
                        
                            const overrides = ctx.overrides;
                        
                            var data = overrides.text_789;
                        
                            const res = core.get(`https://nominatim.openstreetmap.org/search?format=json&limit=1&addressdetails=1&postalcode=${plz}`, {});
                        
                            if (res.statusCode !== 200) {
                                return null;
                            }
                        
                            const body = JSON.parse(res.body);
                        
                            if (body.length === 0 || body[0] == null || body[0].address == null || body[0].address.town == null) {
                                return null;
                            }
                        
                            return body[0].address.town;
                        })();""");

        var references = code.getReferencedIds();

        assertEquals(3, references.size());
        assertTrue(references.contains("text_123"));
        assertTrue(references.contains("text_456"));
        assertTrue(references.contains("text_789"));
    }
}