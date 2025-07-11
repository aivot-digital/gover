package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MultiCheckboxField extends BaseInputElement<List<String>> {
    @Nullable
    private List<MultiCheckboxFieldOption> options;
    @Nullable
    private Integer minimumRequiredOptions;
    @Nullable
    private Boolean displayInline;

    public MultiCheckboxField() {
        super(ElementType.MultiCheckbox);
    }

    @Nullable
    @Override
    public List<String> formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable List<String> value) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            testValuesInOptions(value);
            testRequiredOptionsMet(value);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<String> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Auswahl getroffen";
        }

        return value.stream()
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Keine Auswahl getroffen");
    }

    private void testValuesInOptions(@Nonnull List<String> values) throws ValidationException {
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

        for (String val : values) {
            boolean valueFound = false;
            for (var opt : options) {
                if (Objects.equals(opt.getValue(), val)) {
                    valueFound = true;
                    break;
                }
            }

            if (!valueFound) {
                throw new ValidationException(this, "Ungültige Auswahl: " + val);
            }
        }
    }

    private void testRequiredOptionsMet(@Nonnull List<String> values) throws ValidationException {
        if (minimumRequiredOptions != null && values.size() < minimumRequiredOptions) {
            throw new ValidationException(this, "Mindestens " + minimumRequiredOptions + " Optionen müssen ausgewählt werden.");
        }
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return operator == ConditionOperator.Empty;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof Collection<?> cValA) {
            if (comparedValue instanceof String sValueB) {
                return switch (operator) {
                    case Includes -> cValA.stream().anyMatch(sValueB::equals);
                    case NotIncludes -> cValA.stream().noneMatch(sValueB::equals);
                    default -> false;
                };
            }
        }

        return false;
    }

    @Nullable
    public static List<String> _formatValue(@Nullable Object value) {
        var res = switch (value) {
            case null -> null;
            case String sValue -> List.of(sValue);
            case Collection<?> cValue -> cValue.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
            default -> null;
        };

        return res == null || res.isEmpty() ? null : res;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MultiCheckboxField that = (MultiCheckboxField) o;
        return Objects.equals(options, that.options) && Objects.equals(minimumRequiredOptions, that.minimumRequiredOptions) && Objects.equals(displayInline, that.displayInline);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(options);
        result = 31 * result + Objects.hashCode(minimumRequiredOptions);
        result = 31 * result + Objects.hashCode(displayInline);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public List<MultiCheckboxFieldOption> getOptions() {
        return options;
    }

    public MultiCheckboxField setOptions(@Nullable List<MultiCheckboxFieldOption> options) {
        this.options = options;
        return this;
    }

    @Nullable
    public Integer getMinimumRequiredOptions() {
        return minimumRequiredOptions;
    }

    public MultiCheckboxField setMinimumRequiredOptions(@Nullable Integer minimumRequiredOptions) {
        this.minimumRequiredOptions = minimumRequiredOptions;
        return this;
    }

    @Nullable
    public Boolean getDisplayInline() {
        return displayInline;
    }

    public MultiCheckboxField setDisplayInline(@Nullable Boolean displayInline) {
        this.displayInline = displayInline;
        return this;
    }

    // endregion
}
