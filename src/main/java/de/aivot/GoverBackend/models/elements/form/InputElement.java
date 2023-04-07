package de.aivot.GoverBackend.models.elements.form;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.functions.InputFunctionSet;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class InputElement<T> extends FormElement {
    private String label;
    private String hint;
    private Boolean required;
    private Boolean disabled;
    private T computedValue;
    private InputFunctionSet functionSet;

    protected InputElement(Map<String, Object> data) {
        super(data);

        label = (String) data.get("label");
        hint = (String) data.get("hint");
        required = (Boolean) data.get("required");
        disabled = (Boolean) data.get("disabled");
        computedValue = (T) data.get("computedValue");
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    @Nullable
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Nullable
    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Nullable
    public T getComputedValue() {
        return computedValue;
    }

    public void setComputedValue(T computedValue) {
        this.computedValue = computedValue;
    }

    @Override
    public InputFunctionSet getFunctionSet() {
        return functionSet;
    }

    public void setFunctionSet(InputFunctionSet functionSet) {
        this.functionSet = functionSet;
    }

    @Override
    public boolean isValid(Map<String, Object> customerInput, @Nullable String idPrefix) {
        String id = getId();
        if (idPrefix != null) {
            id = idPrefix + '_' + id;
        }

        Object rawValue = customerInput.get(id);

        if (rawValue == null) {
            return !Boolean.TRUE.equals(required);
        } else {
            T value;

            try {
                value = (T) rawValue;
            } catch (ClassCastException e) {
                return false;
            }

            if (Boolean.TRUE.equals(required)) {
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    return false;
                }
                if (value.getClass().isArray() && Array.getLength(value) == 0) {
                    return false;
                }
                if (value instanceof List<?> && ((List<?>) value).isEmpty()) {
                    return false;
                }
            }

            return isValid(value, idPrefix);
        }
    }

    public abstract boolean isValid(T value, @Nullable String idPrefix);

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        String id = getId();
        if (idPrefix != null) {
            id = idPrefix + '_' + id;
        }

        Object rawValue = customerInput.get(id);
        try {
            T value = (T) rawValue;
            return toPdfRows(value, idPrefix);
        } catch (ClassCastException e) {
            return new LinkedList<>();
        }
    }

    public abstract List<BasePdfRowDto> toPdfRows(T value, @Nullable String idPrefix);
}
