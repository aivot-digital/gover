package de.aivot.GoverBackend.javascript.services;

import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import jakarta.annotation.Nonnull;
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
                    .registerGlobalObject("test", new FormLayoutElement())
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
        @HostAccess.Export
        public String getValue() {
            return "value";
        }

        @HostAccess.Export
        public String echoValue(String value) {
            return value;
        }

        @Override
        public String getObjectName() {
            return "de_aivot_gover_test";
        }

        @Override
        public String[] getMethodTypeDefinitions() {
            return new String[0];
        }

        @Nonnull
        @Override
        public String getParentPluginKey() {
            return "de.aivot";
        }

        @Nonnull
        @Override
        public String getComponentKey() {
            return "test";
        }

        @Nonnull
        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Nonnull
        @Override
        public String getName() {
            return "";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "";
        }
    }
}