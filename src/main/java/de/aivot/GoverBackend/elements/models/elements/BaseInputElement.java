package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Objects;

public abstract class BaseInputElement<T> extends BaseFormElement implements InputElement<T> {
    @Nullable
    private String label;
    @Nullable
    private String hint;
    @Nullable
    private Boolean required;
    @Nullable
    private Boolean disabled;
    @Nullable
    private Boolean technical;
    @Nullable
    private String destinationKey;

    @Nullable
    private ElementValueFunctions value;
    @Nullable
    private ElementValidationFunctions validation;

    public BaseInputElement(ElementType type) {
        super(type);
    }

    public void validate(@Nullable Object rawValue) throws ValidationException {
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

            performValidation(formattedValue);
        }
    }

    @Nonnull
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        return false;
    }

    public abstract void performValidation(@Nullable T value) throws ValidationException;

    @Nullable
    public abstract T formatValue(@Nullable Object value);

    @Override
    public void recalculateReferencedIds() {
        super.recalculateReferencedIds();
        if (value != null) {
            value.recalculateReferencedIds();
        }
        if (validation != null) {
            validation.recalculateReferencedIds();
        }
    }

    @Override
    protected boolean testIfTechnicalApprovalNeeded() {
        var superResult = super.testIfTechnicalApprovalNeeded();

        if (superResult) {
            return true;
        }

        if (validation != null) {
            if (validation.getJavascriptCode() != null && validation.getJavascriptCode().isNotEmpty()) {
                return true;
            }

            if (validation.getNoCodeList() != null && !validation.getNoCodeList().isEmpty()) {
                return true;
            }

            if (validation.getConditionSet() != null) {
                return true;
            }
        }

        if (value != null) {
            if (value.getJavascriptCode() != null && value.getJavascriptCode().isNotEmpty()) {
                return true;
            }

            if (value.getNoCode() != null) {
                return true;
            }
        }

        return false;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseInputElement<?> that = (BaseInputElement<?>) o;
        return Objects.equals(label, that.label) && Objects.equals(hint, that.hint) && Objects.equals(required, that.required) && Objects.equals(disabled, that.disabled) && Objects.equals(technical, that.technical) && Objects.equals(destinationKey, that.destinationKey) && Objects.equals(value, that.value) && Objects.equals(validation, that.validation);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(hint);
        result = 31 * result + Objects.hashCode(required);
        result = 31 * result + Objects.hashCode(disabled);
        result = 31 * result + Objects.hashCode(technical);
        result = 31 * result + Objects.hashCode(destinationKey);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(validation);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getLabel() {
        return label;
    }

    public BaseInputElement<T> setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    @Nullable
    public String getHint() {
        return hint;
    }

    public BaseInputElement<T> setHint(@Nullable String hint) {
        this.hint = hint;
        return this;
    }

    @Nullable
    public Boolean getRequired() {
        return required;
    }

    public BaseInputElement<T> setRequired(@Nullable Boolean required) {
        this.required = required;
        return this;
    }

    @Nullable
    public Boolean getDisabled() {
        return disabled;
    }

    public BaseInputElement<T> setDisabled(@Nullable Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @Nullable
    public Boolean getTechnical() {
        return technical;
    }

    public BaseInputElement<T> setTechnical(@Nullable Boolean technical) {
        this.technical = technical;
        return this;
    }

    @Nullable
    public String getDestinationKey() {
        return destinationKey;
    }

    public BaseInputElement<T> setDestinationKey(@Nullable String destinationKey) {
        this.destinationKey = destinationKey;
        return this;
    }

    @Nullable
    public ElementValueFunctions getValue() {
        return value;
    }

    public BaseInputElement<T> setValue(@Nullable ElementValueFunctions value) {
        this.value = value;
        return this;
    }

    @Nullable
    public ElementValidationFunctions getValidation() {
        return validation;
    }

    public BaseInputElement<T> setValidation(@Nullable ElementValidationFunctions validation) {
        this.validation = validation;
        return this;
    }

    // endregion
}
