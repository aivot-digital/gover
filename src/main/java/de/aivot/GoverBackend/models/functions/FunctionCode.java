package de.aivot.GoverBackend.models.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FunctionCode<T> extends Function<T> {
    private Map<String, String> functions;
    private String mainFunction;

    private final static String jsCodeTemplate = """
                (function() {
                    const $data = %s;
                    const $element = %s;
                    
                    %s
                    
                    %s($data, $element, %s);
                })();
    """;

    public FunctionCode(Map<String, Object> data) {
        super(data);
        functions = MapUtils.get(data, "functions", Map.class);
        mainFunction = MapUtils.getString(data, "mainFunction");
    }

    @Override
    public T evaluate(BaseElement element, Map<String, Object> customerInput, String id, ScriptEngine scriptEngine) {
        var mapper = new ObjectMapper();
        String customerData;
        try {
            customerData = mapper.writeValueAsString(customerInput);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String elementData;
        try {
            elementData = mapper.writeValueAsString(element);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<String> fns = new LinkedList<>();
        for (String fnName : functions.keySet()) {
            fns.add(String.format("function %s%s", fnName, functions.get(fnName)));
        }

        var jsCode = String.format(
                jsCodeTemplate,
                customerData,
                elementData,
                String.join("\n\n", fns),
                mainFunction != null ? mainFunction : "main",
                id);
        try {
            return (T) scriptEngine.eval(jsCode);
        } catch (ScriptException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    //region Getters & Setters
    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }

    public String getMainFunction() {
        return mainFunction;
    }

    public void setMainFunction(String mainFunction) {
        this.mainFunction = mainFunction;
    }
    //endregion
}
