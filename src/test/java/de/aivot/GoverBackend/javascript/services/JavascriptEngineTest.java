package de.aivot.GoverBackend.javascript.services;

import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavascriptEngineTest {
    @Test
    void registerFunctionProvider() {
        List<JavascriptFunctionProvider> serviceProviders = List.of(new TestJavascriptFunctionProvider());

        try (var service = new JavascriptEngine(serviceProviders)) {
            var res = service
                    .evaluateCode(new JavascriptCode().setCode("de_aivot_gover_test.getValue();"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(serviceProviders)) {
            var res = service
                    .evaluateCode(new JavascriptCode().setCode("de_aivot_gover_test.echoValue('value');"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void registerGlobalObject() {
        try (var service = new JavascriptEngine(List.of())) {
            var res = service
                    .registerGlobalObject("test", Map.of("key", "value"))
                    .evaluateCode(new JavascriptCode().setCode("test.key;"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(List.of())) {
            var res = service
                    .registerGlobalObject("test", Map.of("key", Map.of("nestedKey", "value")))
                    .evaluateCode(new JavascriptCode().setCode("test.key.nestedKey;"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(List.of())) {
            var res = service
                    .registerGlobalObject("test", Map.of("key", List.of("value")))
                    .evaluateCode(new JavascriptCode().setCode("test.key[0];"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(List.of())) {
            var res = service
                    .registerGlobalObject("test", new RootElement(Map.of()))
                    .evaluateCode(new JavascriptCode().setCode("test.headline;"));
            assertTrue(res.isNull());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void executeCode() {
        try (var service = new JavascriptEngine(List.of())) {
            var res = service.evaluateCode(new JavascriptCode().setCode("'value'"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(List.of())) {
            var res = service.evaluateCode(new JavascriptCode().setCode("(function() {return 'value';})()"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    public static class TestJavascriptFunctionProvider implements JavascriptFunctionProvider {
        @Override
        public String getPackageName() {
            return "de.aivot.gover.test";
        }

        @Override
        public String getLabel() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @HostAccess.Export
        public String getValue() {
            return "value";
        }

        @HostAccess.Export
        public String echoValue(String value) {
            return value;
        }
    }
}