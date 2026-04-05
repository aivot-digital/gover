package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class SelectInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    @Nullable
    private String autocomplete;

    @Nullable
    private List<SelectInputElementOption> options;

    @Nullable
    private String dependsOnSelectFieldId;

    public SelectInputElement() {
        super(ElementType.Select);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "Keine Auswahl getroffen";
        }

        if (options != null) {
            for (var option : options) {
                if (Objects.equals(option.getValue(), value) && StringUtils.isNotNullOrEmpty(option.getLabel())) {
                    return option.getLabel();
                }
            }
        }

        return value;
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        testValueInOptions(value);
    }

    private void testValueInOptions(@Nullable String value) throws ValidationException {
        if (options == null) {
            throw new ValidationException(this, "Dieses Element hat keine Optionen definiert.");
        }

        var hasInvalidOption = options
                .stream()
                .anyMatch(opt -> (
                        StringUtils.isNullOrEmpty(opt.getValue()) ||
                        StringUtils.isNullOrEmpty(opt.getLabel()))
                );

        if (hasInvalidOption) {
            throw new ValidationException(this, "Eine oder mehrere Optionen sind ungültig (leerer Wert oder Label).");
        }

        if (!containsOptionValue(value)) {
            throw new ValidationException(this, "Ungültige Auswahl: " + value);
        }
    }

    public boolean containsOptionValue(@Nullable String value) {
        if (options == null) {
            return false;
        }

        for (var option : options) {
            if (Objects.equals(option.getValue(), value)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsOptionValueForGroup(@Nullable String value, @Nullable String selectedGroup) {
        if (options == null) {
            return false;
        }

        for (var option : options) {
            if (!Objects.equals(option.getValue(), value)) {
                continue;
            }

            if (StringUtils.isNullOrEmpty(option.getGroup()) || Objects.equals(option.getGroup(), selectedGroup)) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return switch (operator) {
                case Equals -> comparedValue == null;
                case NotEquals -> comparedValue != null;
                case Empty -> true;
                default -> false;
            };
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof String valueA && comparedValue instanceof String valueB) {
            return switch (operator) {
                case Equals -> valueA.equals(valueB);
                case NotEquals -> !valueA.equals(valueB);
                default -> false;
            };
        }

        return false;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SelectInputElement that = (SelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(autocomplete, that.autocomplete)
                && Objects.equals(options, that.options)
                && Objects.equals(dependsOnSelectFieldId, that.dependsOnSelectFieldId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(autocomplete);
        result = 31 * result + Objects.hashCode(options);
        result = 31 * result + Objects.hashCode(dependsOnSelectFieldId);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public SelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public SelectInputElement setAutocomplete(@Nullable String autocomplete) {
        this.autocomplete = autocomplete;
        return this;
    }

    @Nullable
    public List<SelectInputElementOption> getOptions() {
        return options;
    }

    public SelectInputElement setOptions(@Nullable List<SelectInputElementOption> options) {
        this.options = options;
        return this;
    }

    @Nullable
    public String getDependsOnSelectFieldId() {
        return dependsOnSelectFieldId;
    }

    public SelectInputElement setDependsOnSelectFieldId(@Nullable String dependsOnSelectFieldId) {
        this.dependsOnSelectFieldId = dependsOnSelectFieldId;
        return this;
    }

    // endregion
}
