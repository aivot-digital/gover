package de.aivot.GoverBackend.javascript.services;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavascriptEngineFactoryServiceTest {
    @Test
    void testGetEngine() {
        var service = new JavascriptEngineFactoryService(List.of(new TestJavascriptFunctionProvider()));
        var engine = service.getEngine();
        assertNotNull(engine);
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