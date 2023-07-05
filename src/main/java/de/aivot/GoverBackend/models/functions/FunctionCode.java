package de.aivot.GoverBackend.models.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;

public class FunctionCode extends Function {
    private String code;

    private final static String jsCodeTemplate = """
                        (function() {
                            const $data = %s;
                            const $element = %s;
                            
                            %s
                            
                            return main($data, $element, '%s');
                        })();
            """;

    public FunctionCode(Map<String, Object> data) {
        super(data);
        code = MapUtils.getString(data, "code");
    }

    @Override
    public FunctionResult evaluate(String idPrefix, RootElement rootElement, BaseElement element, Map<String, Object> customerInput, ScriptEngine scriptEngine) {
        if (StringUtils.isNullOrEmpty(code)) {
            return null;
        }

        String prefixedId = element.getResolvedId(idPrefix);

        var jsCode = buildJsCode(element, customerInput, prefixedId);

        try {
            Object returnValue = scriptEngine.eval(jsCode);
            if (returnValue != null) {
                return new FunctionResult(returnValue);
            } else {
                return null;
            }
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public String buildJsCode(BaseElement element, Map<String, Object> customerInput, String id) {
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

        return String.format(
                jsCodeTemplate,
                customerData,
                elementData,
                code,
                id);
    }

    //region Getters & Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    //endregion
}
