package de.aivot.prosuna.backend.javascript.services;

import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.javascript.enums.JavascriptExecutionContext;
import de.aivot.prosuna.backend.javascript.exceptions.JavascriptException;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JavascriptEngineTest {
    @Test
    void registerFunctionProvider() {
        List<JavascriptFunctionProvider> serviceProviders = List.of(new TestJavascriptFunctionProvider());

        try (var service = new JavascriptEngine(serviceProviders)) {
            var res = service
                    .evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"));
            assertEquals("value", res.asString());
        } catch (Exception e) {
            fail(e);
        }

        try (var service = new JavascriptEngine(serviceProviders)) {
            var res = service
                    .evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.echoValue('value');"));
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
    void registerGlobalObject_ConvertsInstantToIsoString() {
        var timestamp = Instant.parse("2026-04-14T08:30:00Z");

        try (var service = new JavascriptEngine(List.of())) {
            var res = service
                    .registerGlobalObject("test", Map.of("timestamp", timestamp))
                    .evaluateCode(new JavascriptCode().setCode("test.timestamp;"));
            assertEquals(
                    IsoTimestampUtils.toOffsetString(timestamp),
                    res.asString()
            );
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void mapToProxyObject_ConvertsInstantToIsoString() {
        var timestamp = Instant.parse("2026-04-14T08:30:00Z");

        var proxy = JavascriptEngine.mapToProxyObject(Map.of("timestamp", timestamp));

        assertEquals(IsoTimestampUtils.toOffsetString(timestamp), proxy.getMember("timestamp"));
    }

    @Test
    void collectionToProxyArray_ConvertsInstantToIsoString() {
        var timestamp = Instant.parse("2026-04-14T08:30:00Z");

        var proxy = JavascriptEngine.collectionToProxyArray(List.of(timestamp));

        assertEquals(IsoTimestampUtils.toOffsetString(timestamp), proxy.get(0));
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

    @Test
    void evaluateCode_CancelsCodeThatExceedsTheCpuTimeLimit() throws Exception {
        try (var engine = new JavascriptEngine(List.of())) {
            var exception = assertThrows(
                    JavascriptException.class,
                    () -> engine.evaluateCode(new JavascriptCode().setCode("for (;;) {}"))
            );
            assertTrue(exception.getCause() instanceof PolyglotException polyglotException && polyglotException.isResourceExhausted());
        }
    }

    @Test
    void assetTemplateContext_CancelsRecursionThatCatchesItsStackOverflow() throws Exception {
        var result = new AtomicReference<Throwable>();
        // A small, fixed thread stack, so the frame limit must apply before the catchable RangeError of the JavaScript engine,
        // independent of the stack size of the test thread.
        var thread = new Thread(null, () -> {
            try (var engine = new JavascriptEngine(List.of(), JavascriptExecutionContext.AssetTemplate)) {
                engine.evaluateCode(new JavascriptCode().setCode("(function f() { try { f() } catch (e) { f() } })()"));
            } catch (Throwable e) {
                result.set(e);
            }
        }, "asset-template-recursion", 512 * 1024);
        // The thread must not keep the test JVM alive if the recursion is not cancelled.
        thread.setDaemon(true);
        thread.start();
        thread.join(Duration.ofSeconds(5));

        assertFalse(thread.isAlive(), "The recursion was not cancelled.");
        assertTrue(result.get() instanceof JavascriptException exception
                && exception.getCause() instanceof PolyglotException polyglotException
                && polyglotException.isResourceExhausted(), String.valueOf(result.get()));
    }

    @Test
    void defaultContext_AllowsRecursionDeeperThanTheAssetTemplateFrameLimit() throws Exception {
        var code = "(function f(n) { return n === 0 ? 0 : 1 + f(n - 1) })(300)";

        try (var engine = new JavascriptEngine(List.of(), JavascriptExecutionContext.Default)) {
            assertEquals(300, engine.evaluateCode(new JavascriptCode().setCode(code)).asNumber().intValue());
        }

        try (var engine = new JavascriptEngine(List.of(), JavascriptExecutionContext.AssetTemplate)) {
            var exception = assertThrows(JavascriptException.class, () -> engine.evaluateCode(new JavascriptCode().setCode(code)));
            assertTrue(exception.getCause() instanceof PolyglotException polyglotException && polyglotException.isResourceExhausted());
        }
    }

    @Test
    void assetTemplateContext_OnlyProvidesFunctionProvidersThatOptedIn() throws Exception {
        List<JavascriptFunctionProvider> providers = List.of(
                new TestJavascriptFunctionProvider(),
                new AssetTemplateTestJavascriptFunctionProvider()
        );

        try (var engine = new JavascriptEngine(providers, JavascriptExecutionContext.AssetTemplate)) {
            var res = engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_asset_template_test.getValue();"));
            assertEquals("value", res.asString());

            var exception = assertThrows(
                    JavascriptException.class,
                    () -> engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"))
            );
            assertTrue(exception.getMessage().contains(
                    "Die Funktionen „Test“ (de_aivot_prosuna_test) sind in HTML-Vorlagen aus Dateien & Medien nicht verfügbar."
            ));

            // Other lookups of the global name must not reach the provider either.
            var lookup = engine.evaluateCode(new JavascriptCode().setCode(
                    "try { globalThis['de_aivot_prosuna_test'].getValue(); } catch (e) { 'blocked'; }"
            ));
            assertEquals("blocked", lookup.asString());
        }

        try (var engine = new JavascriptEngine(providers, JavascriptExecutionContext.Default)) {
            var res = engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"));
            assertEquals("value", res.asString());
        }
    }

    @Test
    void assetTemplateContext_BlocksProvidersWithTheSameObjectName() throws Exception {
        List<JavascriptFunctionProvider> providers = List.of(
                new TestJavascriptFunctionProvider(),
                new TestJavascriptFunctionProvider()
        );

        try (var engine = new JavascriptEngine(providers, JavascriptExecutionContext.AssetTemplate)) {
            var exception = assertThrows(
                    JavascriptException.class,
                    () -> engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"))
            );
            assertTrue(exception.getMessage().contains("nicht verfügbar"));
        }
    }

    @Test
    void assetTemplateContext_BlocksObjectNamesSharedByAllowedAndBlockedProviders() throws Exception {
        var allowed = new SharedNameAssetTemplateTestJavascriptFunctionProvider();
        var blocked = new TestJavascriptFunctionProvider();

        try (var engine = new JavascriptEngine(List.of(allowed, blocked), JavascriptExecutionContext.AssetTemplate)) {
            var exception = assertThrows(
                    JavascriptException.class,
                    () -> engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"))
            );
            assertTrue(exception.getMessage().contains("nicht verfügbar"));
        }

        try (var engine = new JavascriptEngine(List.of(blocked, allowed), JavascriptExecutionContext.AssetTemplate)) {
            var exception = assertThrows(
                    JavascriptException.class,
                    () -> engine.evaluateCode(new JavascriptCode().setCode("de_aivot_prosuna_test.getValue();"))
            );
            assertTrue(exception.getMessage().contains("nicht verfügbar"));
        }
    }

    public static class SharedNameAssetTemplateTestJavascriptFunctionProvider extends TestJavascriptFunctionProvider {
        @Override
        public boolean isAvailableIn(@Nonnull JavascriptExecutionContext context) {
            return context == JavascriptExecutionContext.Default || context == JavascriptExecutionContext.AssetTemplate;
        }
    }

    public static class AssetTemplateTestJavascriptFunctionProvider extends TestJavascriptFunctionProvider {
        @Override
        public String getObjectName() {
            return "de_aivot_prosuna_asset_template_test";
        }

        @Override
        public boolean isAvailableIn(@Nonnull JavascriptExecutionContext context) {
            return context == JavascriptExecutionContext.Default || context == JavascriptExecutionContext.AssetTemplate;
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
            return "de_aivot_prosuna_test";
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
            return "Test";
        }

        @Nonnull
        @Override
        public String getAbstract() {
            return "";
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "";
        }
    }
}
