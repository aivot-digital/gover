package de.aivot.GoverBackend.javascript.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.models.JavascriptResult;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

/**
 * Class for executing javascript code.
 * This class should be used, when executing javascript code in the backend.
 * See {@link JavascriptFunctionProvider} for more information.
 * Use the method {@link #registerGlobalObject} to add objects to the javascript context.
 * The objects will be available in the javascript context under the given object name.
 * Use the method {@link #evaluateCode} to evaluate javascript code.
 */
public class JavascriptEngine implements AutoCloseable {
    public static final String JS_CONTEXT_OBJECT_NAME = "ctx";

    private final Context graalContext;
    private final static String JS_ENGINE_NAME = "js";

    /**
     * Creates a new javascript executioner with the given function providers.
     *
     * @param functionProviders the function providers to add to the javascript context.
     */
    public JavascriptEngine(JavascriptFunctionProvider... functionProviders) {
        this(List.of(functionProviders));
    }

    /**
     * Creates a new javascript executioner with the given function providers.
     *
     * @param functionProviders the function providers to add to the javascript context.
     */
    public JavascriptEngine(List<JavascriptFunctionProvider> functionProviders) {
        graalContext = Context
                // Create a new context builder for the javascript engine.
                .newBuilder(JS_ENGINE_NAME)

                // Specify the ecmascript version to use. This is necessary to use the latest features of javascript. Currently, the latest version is 2022.
                .option("js.ecmascript-version", "2022")

                // Remove warning that the engine is only in interpreter mode. TODO: Resolve this problem and remove this option.
                .option("engine.WarnInterpreterOnly", "false")

                // Only allow access to explicitly exported functions and fields. This behavior does not affect the access to proxy objects.
                .allowHostAccess(HostAccess.EXPLICIT)

                // Disable to the following host functions.
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowHostClassLookup(className -> false)
                .allowHostClassLoading(false)
                .allowPolyglotAccess(PolyglotAccess.NONE)

                // Build the context.
                .build();

        // Add the function providers to the javascript context.
        for (var fp : functionProviders) {
            graalContext
                    .getBindings(JS_ENGINE_NAME)
                    .putMember(fp.getObjectName(), fp);
        }
    }

    /**
     * Evaluates the given code and returns the result.
     *
     * @param code the code to evaluate.
     * @return the result of the evaluation.
     */
    public JavascriptResult evaluateCode(JavascriptCode code) {
        if (code == null || code.isEmpty()) {
            return new JavascriptResult(Value.asValue(null));
        }

        var value = graalContext
                .eval(JS_ENGINE_NAME, code.getCode());
        return new JavascriptResult(value);
    }

    public JavascriptEngine registerGlobalContextObject(Object object) {
        return registerGlobalObject(JS_CONTEXT_OBJECT_NAME, object);
    }

    /**
     * Adds a global object to the javascript context by inserting the given object under the given object name.
     * E.g. if the name is "test" and the object has the key "key", the value of the key "key" in the object will be available as test.key.
     * The given object is recursively converted to a proxy object.
     *
     * @param objectName the name of the object in the javascript context.
     * @param object     the object to add to the javascript context.
     * @return this service instance.
     */
    public JavascriptEngine registerGlobalObject(String objectName, Object object) {
        var map = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .convertValue(object, new TypeReference<Map<String, Object>>() {
                });

        var proxy = mapToProxyObject(map);

        graalContext
                .getBindings(JS_ENGINE_NAME)
                .putMember(objectName, proxy);

        return this;
    }

    /**
     * Converts a nested map to a proxy object.
     *
     * @param map the map to convert.
     * @return the proxy object.
     */
    private static ProxyObject mapToProxyObject(Map<?, ?> map) {
        var mutableMap = new HashMap<String, Object>();

        for (var key : map.keySet()) {
            if (key instanceof String sKey) {
                var value = map.get(key);

                if (value instanceof Map<?, ?> childMap) {
                    mutableMap.put(sKey, mapToProxyObject(childMap));
                } else if (value instanceof Collection<?> childCollection) {
                    mutableMap.put(sKey, collectionToProxyArray(childCollection));
                } else {
                    mutableMap.put(sKey, value);
                }
            }
        }

        return ProxyObject.fromMap(mutableMap);
    }

    /**
     * Converts an iterable to a proxy array.
     *
     * @param iterable the iterable to convert.
     * @return the proxy array.
     */
    private static ProxyArray collectionToProxyArray(Collection<?> collection) {
        var mutableList = new ArrayList<>();

        for (var value : collection) {
            if (value instanceof Map<?, ?> childMap) {
                mutableList.add(mapToProxyObject(childMap));
            } else if (value instanceof Collection<?> childCollection) {
                mutableList.add(collectionToProxyArray(childCollection));
            } else {
                mutableList.add(value);
            }
        }

        return ProxyArray.fromList(mutableList);
    }

    @Override
    public void close() throws Exception {
        graalContext.close();
    }
}
