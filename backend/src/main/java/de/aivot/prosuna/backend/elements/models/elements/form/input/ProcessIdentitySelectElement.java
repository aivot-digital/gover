package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ProcessIdentitySelectElement extends BaseInputElement<List<String>> implements PrintableElement<List<String>> {
    @Nullable
    private String placeholder;

    @Nullable
    private Integer minItems;

    @Nullable
    private Integer maxItems;

    public ProcessIdentitySelectElement() {
        super(ElementType.ProcessIdentitySelect);
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

        if (value.stream().distinct().count() != value.size()) {
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
        var result = switch (value) {
            case null -> null;
            case String stringValue -> {
                var normalizedValue = stringValue.trim();
                yield normalizedValue.isEmpty() ? null : List.of(normalizedValue);
            }
            case Collection<?> collectionValue -> collectionValue.stream()
                    .filter(Objects::nonNull)
                    .map(item -> item.toString().trim())
                    .filter(item -> !item.isEmpty())
                    .toList();
            default -> null;
        };

        return result == null || result.isEmpty() ? null : result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.equals(other)) return false;

        ProcessIdentitySelectElement that = (ProcessIdentitySelectElement) other;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(minItems, that.minItems)
                && Objects.equals(maxItems, that.maxItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeholder, minItems, maxItems);
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public ProcessIdentitySelectElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public Integer getMinItems() {
        return minItems;
    }

    public ProcessIdentitySelectElement setMinItems(@Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    @Nullable
    public Integer getMaxItems() {
        return maxItems;
    }

    public ProcessIdentitySelectElement setMaxItems(@Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }
}
