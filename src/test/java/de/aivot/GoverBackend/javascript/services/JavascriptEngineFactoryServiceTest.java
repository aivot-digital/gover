package de.aivot.GoverBackend.javascript.services;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import jakarta.annotation.Nonnull;
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