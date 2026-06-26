package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ChipInputElement extends BaseInputElement<List<String>> implements PrintableElement<List<String>> {
    @Nullable
    private String placeholder;

    @Nullable
    private List<String> suggestions;

    @Nullable
    private Integer minItems;

    @Nullable
    private Integer maxItems;

    @Nullable
    private Boolean allowDuplicates;

    public ChipInputElement() {
        super(ElementType.ChipInput);
    }

    @Nullable
    @Override
    public List<String> formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable List<String> value) throws ValidationException {
        if (value == null) {
            return;
        }

        if (minItems != null && minItems > 0 && value.size() < minItems) {
            throw new ValidationException(this, "Mindestens " + minItems + " Einträge erforderlich.");
        }

        if (maxItems != null && maxItems > 0 && value.size() > maxItems) {
            throw new ValidationException(this, "Maximal " + maxItems + " Einträge erlaubt.");
        }

        if (!Boolean.TRUE.equals(allowDuplicates) && value.stream().distinct().count() != value.size()) {
            throw new ValidationException(this, "Mehrfach vorhandene Einträge sind nicht erlaubt.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<String> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        return value.stream()
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Keine Angabe");
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

            if (comparedValue instanceof Collection<?> cValB) {
                return switch (operator) {
                    case Includes -> cValB.stream().allMatch(cValA::contains);
                    case NotIncludes -> cValB.stream().noneMatch(cValA::contains);
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
            case String sValue -> List.of(sValue.trim());
            case Collection<?> cValue -> cValue.stream()
                    .filter(Objects::nonNull)
                    .map(v -> v.toString().trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
            default -> null;
        };

        return res == null || res.isEmpty() ? null : res;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ChipInputElement that = (ChipInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(suggestions, that.suggestions)
                && Objects.equals(minItems, that.minItems)
                && Objects.equals(maxItems, that.maxItems)
                && Objects.equals(allowDuplicates, that.allowDuplicates);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(suggestions);
        result = 31 * result + Objects.hashCode(minItems);
        result = 31 * result + Objects.hashCode(maxItems);
        result = 31 * result + Objects.hashCode(allowDuplicates);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public ChipInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public List<String> getSuggestions() {
        return suggestions;
    }

    public ChipInputElement setSuggestions(@Nullable List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    @Nullable
    public Integer getMinItems() {
        return minItems;
    }

    public ChipInputElement setMinItems(@Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    @Nullable
    public Integer getMaxItems() {
        return maxItems;
    }

    public ChipInputElement setMaxItems(@Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    @Nullable
    public Boolean getAllowDuplicates() {
        return allowDuplicates;
    }

    public ChipInputElement setAllowDuplicates(@Nullable Boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
        return this;
    }
}
