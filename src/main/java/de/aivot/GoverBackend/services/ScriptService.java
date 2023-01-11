package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ScriptRequiredException;
import de.aivot.GoverBackend.models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
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

        return validateElement(application.getRoot(), customerData, script, null);
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

    @Nullable
    private String validateElement(Map<String, Object> pElement, Map<String, Object> customerData, @Nullable ScriptEngine script, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        Map<String, Object> element = pElement;

        if (!isElementVisible(script, element, idPrefix)) {
            return null;
        }

        String elementId = (String) element.get("id");

        String patchFunc = getFunctionName(element, PATCH_FUNCTION_GROUP);
        if (patchFunc != null) {
            Object scriptRetVal = callFunction(script, element, elementId, patchFunc);
            if (scriptRetVal instanceof Map<?, ?>) {
                element = (Map<String, Object>) scriptRetVal;
            }
        }

        if (idPrefix != null) {
            elementId = idPrefix + "_" + elementId;
        }

        boolean isRequired = Boolean.TRUE.equals(element.getOrDefault("required", false));
        if (isRequired) {
            Object data = customerData.getOrDefault(elementId, null);

            if (data == null) {
                return "Missing value for required element " + elementId;
            }

            if (data instanceof String && Strings.isNullOrEmpty((String) data)) {
                return "Missing or empty value for required element " + elementId;
            }

            if (data instanceof Boolean && Boolean.FALSE.equals(data)) {
                return "False value for required boolean element " + elementId;
            }

            if (data instanceof List<?> && ((List<?>) data).isEmpty()) {
                return "Empty value for required element " + elementId;
            }
        }

        String validateFunc = getFunctionName(element, VALIDATE_FUNCTION_GROUP);
        if (validateFunc != null) {
            Object scriptRetVal = callFunction(script, element, elementId, validateFunc);
            if (scriptRetVal instanceof String && !Strings.isNullOrEmpty((String) scriptRetVal)) {
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

        String elementId = (String) element.get("id");
        if (idPrefix != null) {
            elementId = idPrefix + "_" + elementId;
        }

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
        if (Strings.isNullOrEmpty(funcName)) {
            return null;
        } else {
            return funcName;
        }
    }

    private static Object callFunction(@Nullable ScriptEngine script, Map<String, Object> element, String elementId, String funcName) throws ScriptRequiredException, JsonProcessingException, ScriptException {
        if (script == null) {
            throw new ScriptRequiredException();
        }
        String elementString = mapToString(element);
        String functionCall = String.format("%s(%s, %s, '%s')", funcName, GLOBAL_DATA_FIELD, elementString, elementId);
        return script.eval(functionCall);
    }

    private static String mapToString(Map<String, Object> map) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(map);
    }
}
