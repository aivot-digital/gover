package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class RadioField extends BaseInputElement<String> {
    @Nullable
    private List<RadioFieldOption> options;
    @Nullable
    private Boolean displayInline;

    public RadioField() {
        super(ElementType.Radio);
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

        boolean valueFound = false;
        for (var opt : options) {
            if (Objects.equals(opt.getValue(), value)) {
                valueFound = true;
                break;
            }
        }

        if (!valueFound) {
            throw new ValidationException(this, "Ungültige Auswahl: " + value);
        }
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

        if (referencedValue instanceof String sValA && comparedValue instanceof String sValB) {
            return switch (operator) {
                case Equals -> sValA.equals(sValB);
                case NotEquals -> !sValA.equals(sValB);

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

        RadioField that = (RadioField) o;
        return Objects.equals(options, that.options) && Objects.equals(displayInline, that.displayInline);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(options);
        result = 31 * result + Objects.hashCode(displayInline);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public List<RadioFieldOption> getOptions() {
        return options;
    }

    public RadioField setOptions(@Nullable List<RadioFieldOption> options) {
        this.options = options;
        return this;
    }

    @Nullable
    public Boolean getDisplayInline() {
        return displayInline;
    }

    public RadioField setDisplayInline(@Nullable Boolean displayInline) {
        this.displayInline = displayInline;
        return this;
    }

    // endregion
}
