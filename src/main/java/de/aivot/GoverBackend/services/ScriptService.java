package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private String validateElement(Map<String, Object> element, Map<String, Object> customerData, @Nullable ScriptEngine script, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        String elementId = (String) element.get("id");
        if (idPrefix != null) {
            elementId = idPrefix + "_" + elementId;
        }

        // Store visible state to prevent recalculation in upcoming steps
        boolean elementWasPreviouslyMarkedVisible = false;

        if (element.containsKey("required") && Boolean.TRUE.equals(element.containsKey("required"))) {
            if (isElementVisible(script, element, idPrefix)) {
                elementWasPreviouslyMarkedVisible = true;
                if (!customerData.containsKey(elementId)) {
                    return "Missing required value for " + elementId;
                }
            }
        }

        if (element.containsKey("validate")) {
            Map<String, String> validateFunctionSet = (Map<String, String>) element.get("validate");
            if (validateFunctionSet.containsKey("functionName")) {
                String functionName = validateFunctionSet.get("functionName");
                if (functionName != null && !functionName.isEmpty()) {
                    if (script == null) {
                        throw new ScriptRequiredException();
                    }
                    if (elementWasPreviouslyMarkedVisible || isElementVisible(script, element, idPrefix)) {
                        String elementString = mapToString(element);
                        String functionCall = String.format("%s(%s, %s, '%s')", functionName, GLOBAL_DATA_FIELD, elementString, elementId);
                        return (String) script.eval(functionCall);
                    }
                }
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
        if (!element.containsKey("visibility")) {
            return true;
        }

        String elementString = mapToString(element);

        Map<String, String> visibleFunctionSet = (Map<String, String>) element.get("visibility");
        String functionName = visibleFunctionSet.get("functionName");

        if (functionName != null && !functionName.isEmpty()) {
            if (script == null) {
                throw new ScriptRequiredException();
            }

            String elementId = (String) element.get("id");
            if (idPrefix != null) {
                elementId = idPrefix + "_" + elementId;
            }
            String functionCall = String.format("%s(%s, %s, '%s')", functionName, GLOBAL_DATA_FIELD, elementString, elementId);
            Object result = script.eval(functionCall);

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof String) {
                return "true".equals(result);
            } else {
                return result != null;
            }
        }
        return true;
    }

    private String mapToString(Map<String, Object> map) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(map);
    }
}
