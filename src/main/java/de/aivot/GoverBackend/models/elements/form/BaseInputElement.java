package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.functions.FunctionResult;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.script.ScriptEngine;
import java.lang.reflect.Array;
import java.util.*;

public abstract class BaseInputElement<T> extends BaseFormElement {
    private String label;
    private String hint;
    private Boolean required;
    private Boolean disabled;
    private Boolean technical;
    private Function validate;
    private FunctionCode computeValue;
    private String destinationKey;

    protected BaseInputElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        label = MapUtils.getString(values, "label");
        hint = MapUtils.getString(values, "hint");
        required = MapUtils.getBoolean(values, "required");
        technical = MapUtils.getBoolean(values, "technical");
        disabled = MapUtils.getBoolean(values, "disabled");

        validate = MapUtils.getApply(values, "validate", Map.class, d -> {
            boolean mainFunctionExists = MapUtils.getString(d, "code") != null;
            return mainFunctionExists ? new FunctionCode(d) : new FunctionNoCode(d);
        });

        computeValue = MapUtils.getApply(values, "computeValue", Map.class, FunctionCode::new);

        destinationKey = MapUtils.getString(values, "destinationKey");
    }

    public Optional<T> getComputedValue(RootElement root, Map<String, Object> customerData, String idPrefix, ScriptEngine scriptEngine) {
        if (computeValue == null) {
            return Optional.empty();
        }
        FunctionResult computedValueResult = computeValue.evaluate(idPrefix, root, this, customerData, scriptEngine);
        if (computedValueResult != null) {
            T formattedValue = formatValue(computedValueResult.getObjectValue());
            if (formattedValue != null) {
                return Optional.of(formattedValue);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, ScriptEngine scriptEngine) throws ValidationException {
        Object rawValue = getComputedValue(root, customerInput, idPrefix, scriptEngine).orElse(null);
        if (rawValue == null) {
            rawValue = customerInput.get(getResolvedId(idPrefix));
        }

        if (rawValue == null) {
            if (Boolean.TRUE.equals(required)) {
                throw new ValidationException(this, "Field is required but value was null");
            }
        } else {
            T value = formatValue(rawValue);

            if (Boolean.TRUE.equals(required)) {
                if (value == null) {
                    throw new ValidationException(this, "Field is required but value was null");
                }
                if (value instanceof String && StringUtils.isNullOrEmpty((String) value)) {
                    throw new ValidationException(this, "Field is required but value was null or empty");
                }
                if (value.getClass().isArray() && Array.getLength(value) == 0) {
                    throw new ValidationException(this, "Field is required but value was empty");
                }
                if (value instanceof Collection<?> && ((Collection<?>) value).isEmpty()) {
                    throw new ValidationException(this, "Field is required but value was empty");
                }
            }

            validate(idPrefix, root, customerInput, value, scriptEngine);

            if (validate != null) {
                FunctionResult funcResult = validate.evaluate(idPrefix, root, this, customerInput, scriptEngine);
                Boolean isInvalid = funcResult != null && funcResult.getBooleanValue();
                if (isInvalid) {
                    throw new ValidationException(this, "Validation function failed with: " + funcResult.getStringValue());
                }
            }
        }
    }

    public abstract void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, T value, ScriptEngine scriptEngine) throws ValidationException;

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        if (Boolean.TRUE.equals(technical)) {
            return List.of();
        }

        String id = getId();
        if (idPrefix != null) {
            id = idPrefix + '_' + id;
        }

        Optional<T> computedValue = getComputedValue(root, customerInput, idPrefix, scriptEngine);
        Object rawValue = computedValue.isPresent() ? computedValue.get() : customerInput.get(id);

        T value = formatValue(rawValue);
        return toPdfRows(root, customerInput, value, idPrefix, scriptEngine);
    }

    public abstract List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, T value, String idPrefix, ScriptEngine scriptEngine);

    public abstract T formatValue(Object value);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseInputElement<?> that = (BaseInputElement<?>) o;

        if (!Objects.equals(label, that.label)) return false;
        if (!Objects.equals(hint, that.hint)) return false;
        if (!Objects.equals(required, that.required)) return false;
        if (!Objects.equals(disabled, that.disabled)) return false;
        if (!Objects.equals(technical, that.technical)) return false;
        if (!Objects.equals(validate, that.validate)) return false;
        if (!Objects.equals(computeValue, that.computeValue)) return false;
        return Objects.equals(destinationKey, that.destinationKey);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (hint != null ? hint.hashCode() : 0);
        result = 31 * result + (required != null ? required.hashCode() : 0);
        result = 31 * result + (disabled != null ? disabled.hashCode() : 0);
        result = 31 * result + (technical != null ? technical.hashCode() : 0);
        result = 31 * result + (validate != null ? validate.hashCode() : 0);
        result = 31 * result + (computeValue != null ? computeValue.hashCode() : 0);
        result = 31 * result + (destinationKey != null ? destinationKey.hashCode() : 0);
        return result;
    }

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

    public String getDestinationKey() {
        return destinationKey;
    }

    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
    }

    public Boolean getTechnical() {
        return technical;
    }

    public void setTechnical(Boolean technical) {
        this.technical = technical;
    }
    //endregion
}
