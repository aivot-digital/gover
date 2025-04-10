package de.aivot.GoverBackend.elements.models.form;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.utils.ElementReferenceUtils;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.models.functions.Function;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.models.lib.ValidationExpressionWrapper;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

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

    private List<ValidationExpressionWrapper> validationExpressions;
    private JavascriptCode validationCode;

    private NoCodeExpression valueExpression;
    private JavascriptCode valueCode;

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

        valueCode = JavascriptCode.from(values.get("valueCode"));
        valueExpression = MapUtils.getApply(values, "valueExpression", Map.class, NoCodeExpression::new);

        validationCode = JavascriptCode.from(values.get("validationCode"));
        validationExpressions = MapUtils.getApply(values, "validationExpressions", List.class, l -> {
            List<ValidationExpressionWrapper> wrappers = new ArrayList<>();
            for (Object o : l) {
                if (o instanceof Map) {
                    wrappers.add(new ValidationExpressionWrapper((Map<String, Object>) o));
                }
            }
            return wrappers;
        });

        destinationKey = MapUtils.getString(values, "destinationKey");
    }

    public void validate(String idPrefix, BaseElementDerivationContext context) throws ValidationException {
        Object rawValue = context.getValue(getResolvedId(idPrefix)).orElse(null);

        if (rawValue == null) {
            if (Boolean.TRUE.equals(required)) {
                throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
            }
        } else {
            T formattedValue = formatValue(rawValue);

            if (Boolean.TRUE.equals(required)) {
                if (formattedValue == null) {
                    throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue instanceof String && StringUtils.isNullOrEmpty((String) formattedValue)) {
                    throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue.getClass().isArray() && Array.getLength(formattedValue) == 0) {
                    throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue instanceof Collection<?> && ((Collection<?>) formattedValue).isEmpty()) {
                    throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
            }

            validate(formattedValue);
        }
    }

    public abstract void validate(T value) throws ValidationException;

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String idPrefix, FormState formState) {
        if (Boolean.TRUE.equals(technical)) {
            return List.of();
        }

        var rawValue = formState.values().get(getResolvedId(idPrefix));

        T value = formatValue(rawValue);

        return toPdfRows(root, value, idPrefix, formState);
    }

    public abstract List<BasePdfRowDto> toPdfRows(RootElement root, T value, String idPrefix, FormState formState);

    public abstract T formatValue(Object value);

    public Set<String> getValidationReferencedIds() {
        var set = ElementReferenceUtils
                .getReferencedIds(
                        validationCode,
                        null,
                        validate
                );

        if (validationExpressions != null) {
            for (var validationExpression : validationExpressions) {
                if (validationExpression.getExpression() == null) {
                    continue;
                }

                set.addAll(ElementReferenceUtils
                        .getReferencedIds(
                                null,
                                validationExpression.getExpression(),
                                null
                        ));
            }
        }

        return set;
    }

    public Set<String> getValueReferencedIds() {
        return ElementReferenceUtils
                .getReferencedIds(
                        valueCode,
                        valueExpression,
                        computeValue
                );
    }

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
        if (!Objects.equals(destinationKey, that.destinationKey)) return false;
        if (!Objects.equals(validationCode, that.validationCode)) return false;
        if (!Objects.equals(valueExpression, that.valueExpression)) return false;
        if (!Objects.equals(valueCode, that.valueCode)) return false;
        return Objects.equals(validationExpressions, that.validationExpressions);
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
        result = 31 * result + (validationCode != null ? validationCode.hashCode() : 0);
        result = 31 * result + (valueExpression != null ? valueExpression.hashCode() : 0);
        result = 31 * result + (valueCode != null ? valueCode.hashCode() : 0);
        result = 31 * result + (validationExpressions != null ? validationExpressions.hashCode() : 0);
        return result;
    }

    @Override
    protected boolean testIfTechnicalApprovalNeeded() {
        var superResult = super.testIfTechnicalApprovalNeeded();

        if (superResult) {
            return true;
        }

        if (validationCode != null && validationCode.isNotEmpty()) {
            return true;
        }

        if (validationExpressions != null && !validationExpressions.isEmpty()) {
            return true;
        }

        if (validate != null && validate.isNotEmpty()) {
            return true;
        }

        if (valueCode != null && valueCode.isNotEmpty()) {
            return true;
        }

        if (valueExpression != null && valueExpression.isNotEmpty()) {
            return true;
        }

        if (computeValue != null && computeValue.isNotEmpty()) {
            return true;
        }

        return false;
    }

    // region Getters & Setters

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

    public JavascriptCode getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(JavascriptCode validationCode) {
        this.validationCode = validationCode;
    }

    public NoCodeExpression getValueExpression() {
        return valueExpression;
    }

    public void setValueExpression(NoCodeExpression valueExpression) {
        this.valueExpression = valueExpression;
    }

    public JavascriptCode getValueCode() {
        return valueCode;
    }

    public void setValueCode(JavascriptCode valueCode) {
        this.valueCode = valueCode;
    }

    public List<ValidationExpressionWrapper> getValidationExpressions() {
        return validationExpressions;
    }

    public void setValidationExpressions(List<ValidationExpressionWrapper> validationExpressions) {
        this.validationExpressions = validationExpressions;
    }

    // endregion
}
