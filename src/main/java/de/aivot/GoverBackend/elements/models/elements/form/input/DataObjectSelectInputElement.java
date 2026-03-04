package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class DataObjectSelectInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    @Nullable
    private String dataModelKey;

    @Nullable
    private String dataLabelAttributeKey;

    public DataObjectSelectInputElement() {
        super(ElementType.DataObjectSelect);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        if (value == null) {
            return;
        }

        if (StringUtils.isNullOrEmpty(dataModelKey)) {
            throw new ValidationException(this, "Für die Datenobjekt-Auswahl muss ein Datenmodell konfiguriert sein.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return "Keine Angabe";
        }

        return value;
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = formatValue(referencedValue);
        var valueB = formatValue(comparedValue);

        return switch (operator) {
            case Equals -> Objects.equals(valueA, valueB);
            case NotEquals -> !Objects.equals(valueA, valueB);
            case Empty -> StringUtils.isNullOrEmpty(valueA);
            case NotEmpty -> StringUtils.isNotNullOrEmpty(valueA);
            default -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DataObjectSelectInputElement that = (DataObjectSelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(dataModelKey, that.dataModelKey)
                && Objects.equals(dataLabelAttributeKey, that.dataLabelAttributeKey);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(dataModelKey);
        result = 31 * result + Objects.hashCode(dataLabelAttributeKey);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DataObjectSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getDataModelKey() {
        return dataModelKey;
    }

    public DataObjectSelectInputElement setDataModelKey(@Nullable String dataModelKey) {
        this.dataModelKey = dataModelKey;
        return this;
    }

    @Nullable
    public String getDataLabelAttributeKey() {
        return dataLabelAttributeKey;
    }

    public DataObjectSelectInputElement setDataLabelAttributeKey(@Nullable String dataLabelAttributeKey) {
        this.dataLabelAttributeKey = dataLabelAttributeKey;
        return this;
    }
}
