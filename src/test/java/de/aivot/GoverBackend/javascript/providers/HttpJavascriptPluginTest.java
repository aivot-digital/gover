package de.aivot.GoverBackend.javascript.providers;

import de.aivot.GoverBackend.core.javascript.HttpJavascriptFunctionProvider;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class HttpJavascriptPluginTest {

    @Test
    void get() {
        try (var jsService = new JavascriptEngine(new HttpJavascriptFunctionProvider())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("de_aivot_gover_http.get('https://postman-echo.com/get?test=value', {'x-test-header': 'header-value'}).body;"));

            var json = new JSONObject(result.asString());

            assertEquals(json.getJSONObject("args").getString("test"), "value");
            assertEquals(json.getJSONObject("headers").getString("x-test-header"), "header-value");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void post() {
        try (var jsService = new JavascriptEngine(new HttpJavascriptFunctionProvider())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("de_aivot_gover_http.post('https://postman-echo.com/post?test=value', JSON.stringify({'test-field': 'field-value'}), {'x-test-header': 'header-value'}).body;"));

            var json = new JSONObject(result.asString());

            assertEquals(json.getJSONObject("args").getString("test"), "value");
            assertEquals(json.getJSONObject("data").getString("test-field"), "field-value");
            assertEquals(json.getJSONObject("headers").getString("x-test-header"), "header-value");
        } catch (Exception e) {
            fail(e);
        }
    }
}