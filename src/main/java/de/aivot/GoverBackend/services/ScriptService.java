package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ScriptRequiredException;
import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class ScriptService {
    private static final Logger logger = LoggerFactory.getLogger(ScriptService.class);

    private final static String GLOBAL_DATA_FIELD = "$$global";
    private final static String PATCH_FUNCTION_GROUP = "patch";
    private final static String VALIDATE_FUNCTION_GROUP = "validate";
    private final static String VISIBILITY_FUNCTION_GROUP = "visibility";
    private final static String FUNCTION_NAME_KEY = "functionName";

    private final BlobService blobService;

    @Autowired
    public ScriptService(BlobService blobService) {
        this.blobService = blobService;
    }

    @Nullable
    public String validateApplication(Application application, Map<String, Object> customerData) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        ScriptEngine script = null;
        try {
            script = prepareEngine(application, customerData);
        } catch (IOException e) {
            logger.info("No ScriptEngine loaded");
        }
        return null; //validateElement(application.getRoot(), customerData, script, null);
    }

    private ScriptEngine prepareEngine(Application application, Map<String, Object> customerData) throws IOException, ScriptException {
        Path codePath = blobService.getCodePath(application);

        BufferedReader in = Files.newBufferedReader(codePath);

        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        engine.eval(in);

        String globalData = mapToString(customerData);
        engine.eval(String.format("const %s = %s;", GLOBAL_DATA_FIELD, globalData));

        return engine;
    }

    /*
    @Nullable
    private String validateElement(BaseElement element, Map<String, Object> customerData, @Nullable ScriptEngine script, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        if (!isElementVisible(script, element, idPrefix)) {
            return null;
        }

        element = patchElement(script, element, idPrefix);

        String elementId = getPrefixedId(element, idPrefix);

        boolean isRequired = Boolean.TRUE.equals(element.getOrDefault("required", false));
        if (isRequired) {
            Object value = getElementValue(customerData, element, idPrefix);

            if (value == null) {
                return "Missing value for required element " + elementId;
            }

            if (value instanceof String && StringUtils.isNullOrEmpty((String) value)) {
                return "Missing or empty value for required element " + elementId;
            }

            if (value instanceof Boolean && Boolean.FALSE.equals(value)) {
                return "False value for required boolean element " + elementId;
            }

            if (value instanceof List<?> && ((List<?>) value).isEmpty()) {
                return "Empty value for required element " + elementId;
            }
        }

        String validateFunc = getFunctionName(element, VALIDATE_FUNCTION_GROUP);
        if (validateFunc != null) {
            Object scriptRetVal = callFunction(script, element, elementId, validateFunc);
            if (scriptRetVal instanceof String && StringUtils.isNotNullOrEmpty((String) scriptRetVal)) {
                return (String) scriptRetVal;
            }
        }

        if (element.containsKey("children")) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");
            if (ElementType.ReplicatingContainer.matches(element.get("type"))) {
                List<String> childIds = (List<String>) customerData.get(elementId);
                if (childIds != null) {
                    for (String childId : childIds) {
                        for (Map<String, Object> child : children) {
                            String result = validateElement(child, customerData, script, elementId + "_" + childId);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }
            } else {
                for (Map<String, Object> child : children) {
                    String result = validateElement(child, customerData, script, idPrefix);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    public static Object getElementValue(Map<String, Object> customerData, Map<String, Object> element, String idPrefix) {
        String elementId = getPrefixedId(element, idPrefix);
        Object patchedValue = element.getOrDefault("value", null);
        if (patchedValue != null) {
            return patchedValue;
        }
        return customerData.getOrDefault(elementId, null);
    }

    public Map<String, Object> patchElement(Application application, Map<String, Object> element, Map<String, Object> customerData, @Nullable String idPrefix) throws ScriptException, ScriptRequiredException, JsonProcessingException {
        ScriptEngine script = null;
        try {
            script = prepareEngine(application, customerData);
        } catch (IOException e) {
            logger.info("No ScriptEngine loaded");
        }
        return patchElement(script, element, idPrefix);
    }

    private Map<String, Object> patchElement(@Nullable ScriptEngine script, Map<String, Object> element, String idPrefix) throws ScriptRequiredException, JsonProcessingException, ScriptException {
        String elementId = getPrefixedId(element, idPrefix);
        String patchFunc = getFunctionName(element, PATCH_FUNCTION_GROUP);
        if (patchFunc != null) {
            Object scriptRetVal = callFunction(script, element, elementId, patchFunc);
            if (scriptRetVal instanceof Map<?, ?>) {
                element = (Map<String, Object>) scriptRetVal;
            }
        }
        return element;
    }

    public boolean isElementVisible(Application application, Map<String, Object> element, Map<String, Object> customerData, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        ScriptEngine script = null;
        try {
            script = prepareEngine(application, customerData);
        } catch (IOException e) {
            logger.info("No ScriptEngine loaded");
        }
        return isElementVisible(script, element, idPrefix);
    }

    public boolean isElementVisible(@Nullable ScriptEngine script, Map<String, Object> element, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        String functionName = getFunctionName(element, VISIBILITY_FUNCTION_GROUP);
        if (functionName == null) {
            return true;
        }

        String elementId = getPrefixedId(element, idPrefix);

        Object result = callFunction(script, element, elementId, functionName);

        if (result instanceof Boolean) {
            return (Boolean) result;
        } else if (result instanceof String) {
            return "true".equals(result);
        } else {
            return result != null;
        }
    }

    @Nullable
    private static String getFunctionName(Map<String, Object> element, String group) {
        if (!element.containsKey(group)) {
            return null;
        }

        Map<String, String> functionGroup = (Map<String, String>) element.get(group);
        String funcName = functionGroup.getOrDefault(FUNCTION_NAME_KEY, null);

        return StringUtils.isNullOrEmpty(funcName) ? null : funcName;
    }

    private static Object callFunction(@Nullable ScriptEngine script, Map<String, Object> element, String elementId, String funcName) throws ScriptRequiredException, JsonProcessingException, ScriptException {
        if (script == null) {
            throw new ScriptRequiredException();
        }
        String elementString = mapToString(element);
        String functionCall = String.format("%s(%s, %s, '%s')", funcName, GLOBAL_DATA_FIELD, elementString, elementId);
        return script.eval(functionCall);
    }
*/
    private static String mapToString(Map<String, Object> map) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(map);
    }

    /*
    public static String getPrefixedId(@NotNull Map<String, Object> element, @Nullable String idPrefix) {
        String elementId = (String) element.get("id");
        if (idPrefix != null) {
            elementId = idPrefix + "_" + elementId;
        }
        return elementId;
    }
     */
}
