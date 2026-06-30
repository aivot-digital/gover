package de.aivot.gover.backend.javascript.providers;

import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.plugins.core.v1.javascript.NumberJavascriptV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class NumberJavascriptPluginTest {
    @Test
    void formatGermanNumber() {
        try (var jsService = new JavascriptEngine(new NumberJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _number_v1.formatGermanNumber(1234.5),
                        _number_v1.formatGermanNumber(1234.567, 1),
                        _number_v1.formatGermanNumber(1234.567, -1)
                    ];
                    """));

            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("1.234,50", values.get(0));
            assertEquals("1.234,6", values.get(1));
            assertEquals("1.234,57", values.get(2));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void parseGermanNumber() {
        try (var jsService = new JavascriptEngine(new NumberJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("_number_v1.parseGermanNumber('1.234,56');"));

            var number = assertInstanceOf(Number.class, result.asNumber());
            assertEquals(1234.56, number.doubleValue(), 0.001);
        } catch (Exception e) {
            fail(e);
        }
    }
}
