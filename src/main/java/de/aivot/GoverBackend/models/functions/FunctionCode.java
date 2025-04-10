package de.aivot.GoverBackend.models.functions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    public FunctionCode() {
        super();
    }

    @Override
    public FunctionResult evaluate(String idPrefix, BaseElement element, BaseElementDerivationContext context) {
        if (StringUtils.isNullOrEmpty(code)) {
            return null;
        }

        String prefixedId = element.getResolvedId(idPrefix);

        var jsCode = buildJsCode(element, context.getCombinedValues(), prefixedId);

        try {
            Object returnValue = context.getLegacyJavascriptEngine().eval(jsCode);
            if (returnValue != null) {
                return new FunctionResult(returnValue, false);
            } else {
                return new FunctionResult(null, false);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FunctionCode that = (FunctionCode) o;
        return Objects.equals(code, that.code) && super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(code);
        result = 31 * result + super.hashCode();
        return result;
    }

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isNullOrEmpty(code);
    }

    @NotNull
    @Override
    public Set<String> getReferencedIds() {
        if (code == null || StringUtils.isNullOrEmpty(code)) {
            return new HashSet<>();
        }

        var referencedIds = new HashSet<String>();

        var expliciteReferencePattern = java.util.regex.Pattern.compile(">>>([a-zA-Z0-9_-]+)");
        var implicitReferencePattern = java.util.regex.Pattern.compile("data\\.([a-zA-Z0-9_-]+)");

        var matcher = expliciteReferencePattern.matcher(code);
        while (matcher.find()) {
            referencedIds.add(matcher.group(1));
        }

        matcher = implicitReferencePattern.matcher(code);
        while (matcher.find()) {
            referencedIds.add(matcher.group(1));
        }

        return referencedIds;
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
