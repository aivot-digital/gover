package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.FunctionResult;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.script.ScriptEngine;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class BaseInputElement<T> extends BaseFormElement {
    private String label;
    private String hint;
    private Boolean required;
    private Boolean disabled;
    private Function validate;
    private FunctionCode computeValue;

    protected BaseInputElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        label = MapUtils.getString(values, "label");
        hint = MapUtils.getString(values, "hint");
        required = MapUtils.getBoolean(values, "required");
        disabled = MapUtils.getBoolean(values, "disabled");

        validate = MapUtils.getApply(values, "validate", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "code") != null;
            return mainFunctionExists ? new FunctionCode(d) : new FunctionNoCode(d);
        });

        computeValue = MapUtils.getApply(values, "computeValue", Map.class, FunctionCode::new);
    }

    public Optional<T> getComputedValue(Map<String, Object> customerData, String idPrefix, ScriptEngine scriptEngine) {
        if (computeValue == null) {
            return Optional.empty();
        }
        FunctionResult computedValueResult = computeValue.evaluate(this, customerData, getResolvedId(idPrefix), scriptEngine);
        if (computedValueResult != null) {
            try {
                return Optional.of((T) computedValueResult.getObjectValue());
            } catch (ClassCastException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        Object rawValue = getComputedValue(customerInput, idPrefix, scriptEngine).orElse(null);
        if (rawValue == null) {
            rawValue = customerInput.get(getResolvedId(idPrefix));
        }

        if (rawValue == null) {
            if (Boolean.TRUE.equals(required)) {
                throw new ValidationException(this, "Field is required but value was null");
            }
        } else {
            T value;

            try {
                value = (T) rawValue;
            } catch (ClassCastException e) {
                throw new ValidationException(this, "Cannot cast value type of field to excepted type: " + e.getMessage());
            }

            if (Boolean.TRUE.equals(required)) {
                if (value instanceof String && StringUtils.isNullOrEmpty((String) value)) {
                    throw new ValidationException(this, "Field is required but value was null or empty");
                }
                if (value.getClass().isArray() && Array.getLength(value) == 0) {
                    throw new ValidationException(this, "Field is required but value was empty");
                }
                if (value instanceof List<?> && ((List<?>) value).isEmpty()) {
                    throw new ValidationException(this, "Field is required but value was empty");
                }
            }

            if (value instanceof Integer) {
                value = (T) Double.valueOf(((Integer) value).doubleValue());
            }

            validate(customerInput, value, idPrefix, scriptEngine);

            if (validate != null) {
                FunctionResult funcResult = validate.evaluate(this, customerInput, getResolvedId(idPrefix), scriptEngine);
                Boolean isInvalid = funcResult != null && funcResult.getBooleanValue();
                if (isInvalid) {
                    throw new ValidationException(this, "Validation function failed with: " + funcResult.getStringValue());
                }
            }
        }
    }

    public abstract void validate(Map<String, Object> customerInput, T value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException;

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        String id = getId();
        if (idPrefix != null) {
            id = idPrefix + '_' + id;
        }

        Optional<T> computedValue = getComputedValue(customerInput, idPrefix, scriptEngine);
        Object rawValue = computedValue.isPresent() ? computedValue.get() :  customerInput.get(id);
        try {
            T value = (T) rawValue;
            return toPdfRows(customerInput, value, idPrefix, scriptEngine);
        } catch (ClassCastException e) {
            return new LinkedList<>();
        }
    }

    public abstract List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, T value, String idPrefix, ScriptEngine scriptEngine);

    //region Getters & Setters
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Function getValidate() {
        return validate;
    }

    public void setValidate(Function validate) {
        this.validate = validate;
    }

    public FunctionCode getComputeValue() {
        return computeValue;
    }

    public void setComputeValue(FunctionCode computeValue) {
        this.computeValue = computeValue;
    }
    //endregion
}
