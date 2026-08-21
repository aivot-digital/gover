package de.aivot.prosuna.backend.javascript.services;

import com.fasterxml.jackson.core.type.TypeReference;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.javascript.exceptions.JavascriptException;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.models.JavascriptResult;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Class for executing javascript code. This class should be used, when executing javascript code in the backend. See {@link JavascriptFunctionProvider} for more information. Use
 * the method {@link #registerGlobalObject} to add objects to the javascript context. The objects will be available in the javascript context under the given object name. Use the
 * method {@link #evaluateCode} to evaluate javascript code.
 */
public class JavascriptEngine implements AutoCloseable {
    public static final String JS_CONTEXT_OBJECT_NAME = "ctx";
    public static final String JS_ELEMENT_OBJECT_NAME = "element";

    private final Context graalContext;
    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
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

                // Set the Sandbox policy to ISOLATED for:
                //   - Disallows native access.
                //   - Disallows process creation.
                //   - Disallows system exit, prohibiting the guest code from terminating the entire VM where this is supported by the language.
                //   - Requires redirection of the standard output and error streams. This is to mitigate risks where external components, such as log processing, may be confused by unexpected writes to output streams by guest code.
                //   - Disallows host file or socket access. Only custom polyglot file system implementations are allowed.
                //   - Disallows environment access.
                //   - Restricts host access:
                //        - Disallows host class loading.
                //        - Disallows access to all public host classes and methods by default.
                //        - Disallows access inheritance.
                //        - Disallows implementation of arbitrary host classes and interfaces.
                //        - Disallows implementation of java.lang.FunctionalInterface.
                //        - Disallows host object mappings of mutable target types. The HostAccess.CONSTRAINED host access policy is preconfigured to fulfill the requirements for the CONSTRAINED sandboxing policy.
                .sandbox(SandboxPolicy.CONSTRAINED)

                // Specify the ecmascript version to use.
                // This is necessary to use the latest features of javascript.
                // Currently, the latest version is 2025.
                .option("js.ecmascript-version", "2025")

                // Remove warning that the engine is only in interpreter mode.
                // TODO: Resolve this problem and remove this option.
                .option("engine.WarnInterpreterOnly", "false")

                // Redirect the standard output and error streams to the given output streams.
                .out(outStream)
                .err(errStream)

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
    public JavascriptResult evaluateCode(JavascriptCode code) throws JavascriptException {
        if (code == null || code.isEmpty() || code.getCode() == null) {
            return new JavascriptResult(Value.asValue(null), "", "");
        }

        try {
            var value = graalContext
                    .eval(JS_ENGINE_NAME, code.getCode());

            var out = outStream.toString(StandardCharsets.UTF_8);
            outStream.reset();
            var err = errStream.toString(StandardCharsets.UTF_8);
            errStream.reset();

            return new JavascriptResult(value, out, err);
        } catch (PolyglotException e) {
            throw new JavascriptException(e);
        }
    }

    public JavascriptEngine registerGlobalContextObject(Object object) {
        return registerGlobalObject(JS_CONTEXT_OBJECT_NAME, object);
    }

    public JavascriptEngine registerElementObject(BaseElement element) {
        return registerGlobalObject(JS_ELEMENT_OBJECT_NAME, element);
    }

    public JavascriptEngine registerProcessExecutionData(ProcessExecutionData processExecutionData) {
        for (var key : ProcessExecutionData.PROCESS_EXEC_DATA_KEYS) {
            if (processExecutionData.containsKey(key)) {
                this.registerGlobalObject(key, processExecutionData.get(key));
            }
        }
        return this;
    }

    /**
     * Adds a global object to the javascript context by inserting the given object under the given object name. E.g. if the name is "test" and the object has the key "key", the
     * value of the key "key" in the object will be available as test.key. The given object is recursively converted to a proxy object.
     *
     * @param objectName the name of the object in the javascript context.
     * @param object     the object to add to the javascript context.
     * @return this service instance.
     */
    public JavascriptEngine registerGlobalObject(String objectName, Object object) {
        var map = JsonMapperFactory
                .getInstance()
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
    public static ProxyObject mapToProxyObject(Map<?, ?> map) {
        var mutableMap = new HashMap<String, Object>();

        for (var key : map.keySet()) {
            if (key instanceof String sKey) {
                var value = map.get(key);

                switch (value) {
                    case Map<?, ?> childMap -> mutableMap.put(sKey, mapToProxyObject(childMap));
                    case Collection<?> childCollection -> mutableMap.put(sKey, collectionToProxyArray(childCollection));
                    case Instant instant -> mutableMap.put(sKey, IsoTimestampUtils.toOffsetString(instant));

                    case BigDecimal number -> mutableMap.put(sKey, number.doubleValue());
                    case Double number -> mutableMap.put(sKey, number.doubleValue());
                    case Float number -> mutableMap.put(sKey, number.doubleValue());

                    case BigInteger number -> mutableMap.put(sKey, number.intValue());
                    case Integer number -> mutableMap.put(sKey, number.intValue());
                    case Long number -> mutableMap.put(sKey, number.intValue());
                    case Short number -> mutableMap.put(sKey, number.intValue());

                    case Number number -> mutableMap.put(sKey, number.doubleValue());

                    case null, default -> mutableMap.put(sKey, value);
                }
            }
        }

        return ProxyObject.fromMap(mutableMap);
    }

    /**
     * Converts an iterable to a proxy array.
     *
     * @param collection the iterable to convert.
     * @return the proxy array.
     */
    public static ProxyArray collectionToProxyArray(Collection<?> collection) {
        var mutableList = new ArrayList<>();

        for (var value : collection) {
            switch (value) {
                case Map<?, ?> childMap -> mutableList.add(mapToProxyObject(childMap));
                case Collection<?> childCollection -> mutableList.add(collectionToProxyArray(childCollection));
                case Instant instant -> mutableList.add(IsoTimestampUtils.toOffsetString(instant));

                case BigDecimal number -> mutableList.add(number.doubleValue());
                case Double number -> mutableList.add(number.doubleValue());
                case Float number -> mutableList.add(number.doubleValue());

                case BigInteger number -> mutableList.add(number.intValue());
                case Integer number -> mutableList.add(number.intValue());
                case Long number -> mutableList.add(number.intValue());
                case Short number -> mutableList.add(number.intValue());

                case Number number -> mutableList.add(number.doubleValue());

                case null, default -> mutableList.add(value);
            }
        }

        return ProxyArray.fromList(mutableList);
    }

    @Override
    public void close() throws Exception {
        outStream.close();
        errStream.close();
        graalContext.close();
    }
}
