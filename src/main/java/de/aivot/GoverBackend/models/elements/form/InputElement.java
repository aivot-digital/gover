package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.script.ScriptEngine;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class InputElement<T> extends FormElement {
    private String label;
    private String hint;
    private Boolean required;
    private Boolean disabled;
    private Function<String> isValid;
    private Function<Boolean> isDisabled;
    private Function<Boolean> isRequired;
    private FunctionCode<T> computeValue;

    protected InputElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        label = MapUtils.getString(values, "label");
        hint = MapUtils.getString(values, "hint");
        required = MapUtils.getBoolean(values, "required");
        disabled = MapUtils.getBoolean(values, "disabled");

        isValid = MapUtils.getApply(values, "isValid", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "mainFunction") != null;
            return mainFunctionExists ? new FunctionCode<String>(d) : new FunctionNoCode<String>(d);
        });

        isDisabled = MapUtils.getApply(values, "isDisabled", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "mainFunction") != null;
            return mainFunctionExists ? new FunctionCode<Boolean>(d) : new FunctionNoCode<Boolean>(d);
        });

        isRequired = MapUtils.getApply(values, "isRequired", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "mainFunction") != null;
            return mainFunctionExists ? new FunctionCode<Boolean>(d) : new FunctionNoCode<Boolean>(d);
        });

        computeValue = MapUtils.getApply(values, "computeValue", Map.class, FunctionCode::new);
    }

    public Optional<T> getComputedValue(Map<String, Object> customerData, String idPrefix, ScriptEngine scriptEngine) {
        if (computeValue == null) {
            return Optional.empty();
        }
        T computedValue = computeValue.evaluate(this, customerData, getResolvedId(idPrefix), scriptEngine);
        return computedValue != null ? Optional.of(computedValue) : Optional.empty();
    }

    @Override
    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        Object rawValue = getComputedValue(customerInput, idPrefix, scriptEngine).orElse(null);
        if (rawValue == null) {
            rawValue = customerInput.get(getResolvedId(idPrefix));
        }

        if (rawValue == null) {
            // TODO: Check required function
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

            validate(customerInput, value, idPrefix, scriptEngine);

            if (isValid != null) {
                String funcResult = isValid.evaluate(this, customerInput, getResolvedId(idPrefix), scriptEngine);
                if (funcResult != null) {
                    throw new ValidationException(this, "Validation function failed with: " + funcResult);
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

        Object rawValue = customerInput.get(id);
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

    public Function<String> getIsValid() {
        return isValid;
    }

    public void setIsValid(Function<String> isValid) {
        this.isValid = isValid;
    }

    public Function<Boolean> getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Function<Boolean> isDisabled) {
        this.isDisabled = isDisabled;
    }

    public Function<Boolean> getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Function<Boolean> isRequired) {
        this.isRequired = isRequired;
    }

    public FunctionCode<T> getComputeValue() {
        return computeValue;
    }

    public void setComputeValue(FunctionCode<T> computeValue) {
        this.computeValue = computeValue;
    }
    //endregion
}
