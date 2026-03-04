package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DomainAndUserSelectInputElement extends BaseInputElement<List<DomainAndUserSelectInputElementValue>> implements PrintableElement<List<DomainAndUserSelectInputElementValue>> {
    @Nullable
    private String placeholder;

    @Nullable
    private Integer minItems;

    @Nullable
    private Integer maxItems;

    @Nullable
    private List<String> allowedTypes;

    @Nullable
    private DomainAndUserSelectProcessAccessConstraint processAccessConstraint;

    public DomainAndUserSelectInputElement() {
        super(ElementType.DomainAndUserSelect);
    }

    @Nullable
    @Override
    public List<DomainAndUserSelectInputElementValue> formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable List<DomainAndUserSelectInputElementValue> value) throws ValidationException {
        if (processAccessConstraint != null) {
            var processId = processAccessConstraint.getProcessId();
            var processVersion = processAccessConstraint.getProcessVersion();

            if (processId == null || processVersion == null) {
                throw new ValidationException(this, "Die Prozessbeschränkung muss Prozess-ID und Prozessversion enthalten.");
            }

            if (processId <= 0 || processVersion <= 0) {
                throw new ValidationException(this, "Prozess-ID und Prozessversion müssen größer als 0 sein.");
            }
        }

        if (value == null) {
            return;
        }

        if (minItems != null && minItems > 0 && value.size() < minItems) {
            throw new ValidationException(this, "Mindestens " + minItems + " Einträge erforderlich.");
        }

        if (maxItems != null && maxItems > 0 && value.size() > maxItems) {
            throw new ValidationException(this, "Maximal " + maxItems + " Einträge erlaubt.");
        }

        var normalizedValues = value
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (normalizedValues.stream().anyMatch(v -> !isAllowedType(v.getType()) || v.getId() == null || v.getId().isBlank())) {
            throw new ValidationException(this, "Ungültiger Eintrag in der Auswahl.");
        }

        if (normalizedValues.stream().anyMatch(v -> !isTypeEnabled(v.getType()))) {
            throw new ValidationException(this, "Eintragstyp ist laut Konfiguration nicht erlaubt.");
        }

        var distinctKeys = normalizedValues
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .distinct()
                .count();

        if (distinctKeys != normalizedValues.size()) {
            throw new ValidationException(this, "Mehrfach vorhandene Einträge sind nicht erlaubt.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<DomainAndUserSelectInputElementValue> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        return value
                .stream()
                .filter(Objects::nonNull)
                .map(DomainAndUserSelectInputElement::formatDisplayToken)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Keine Angabe");
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = _formatValue(referencedValue);

        if (valueA == null || valueA.isEmpty()) {
            return operator == ConditionOperator.Empty || operator == ConditionOperator.NotIncludes;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (operator == ConditionOperator.Empty) {
            return false;
        }

        var valueAKeys = valueA
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());

        var comparedKeys = _formatComparedKeys(comparedValue);
        if (comparedKeys.isEmpty()) {
            return operator == ConditionOperator.NotIncludes;
        }

        return switch (operator) {
            case Includes -> comparedKeys.stream().allMatch(valueAKeys::contains);
            case NotIncludes -> comparedKeys.stream().allMatch(key -> !valueAKeys.contains(key));
            default -> false;
        };
    }

    @Nullable
    public static List<DomainAndUserSelectInputElementValue> _formatValue(@Nullable Object value) {
        var values = switch (value) {
            case null -> null;
            case DomainAndUserSelectInputElementValue val -> List.of(val);
            case Map<?, ?> map -> List.of(_formatSingleValue(map));
            case String sValue -> {
                var parsed = _parseLegacyToken(sValue);
                yield parsed == null ? null : List.of(parsed);
            }
            case Collection<?> cValue -> {
                var result = new ArrayList<DomainAndUserSelectInputElementValue>();
                for (var item : cValue) {
                    var parsed = _formatSingleValue(item);
                    if (parsed != null) {
                        result.add(parsed);
                    }
                }
                yield result;
            }
            default -> null;
        };

        if (values == null || values.isEmpty()) {
            return null;
        }

        var normalized = values
                .stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.isEmpty())
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }

    @Nullable
    private static DomainAndUserSelectInputElementValue _formatSingleValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case DomainAndUserSelectInputElementValue val -> val;
            case String sValue -> _parseLegacyToken(sValue);
            case Map<?, ?> map -> {
                var type = map.get("type") == null ? null : map.get("type").toString();
                var id = map.get("id") == null ? null : map.get("id").toString();
                if (type == null || id == null || type.isBlank() || id.isBlank()) {
                    yield null;
                }

                yield new DomainAndUserSelectInputElementValue(type, id);
            }
            default -> null;
        };
    }

    @Nullable
    private static DomainAndUserSelectInputElementValue _parseLegacyToken(@Nullable String token) {
        if (token == null) {
            return null;
        }

        var normalized = token.trim();
        if (normalized.isBlank()) {
            return null;
        }

        var separatorIndex = normalized.indexOf(':');
        if (separatorIndex < 0 || separatorIndex >= normalized.length() - 1) {
            return null;
        }

        var type = normalized.substring(0, separatorIndex).trim();
        var id = normalized.substring(separatorIndex + 1).trim();

        if (!isAllowedType(type) || id.isBlank()) {
            return null;
        }

        return new DomainAndUserSelectInputElementValue(type, id);
    }

    @Nonnull
    private static Set<String> _formatComparedKeys(@Nullable Object comparedValue) {
        var formattedValue = _formatValue(comparedValue);
        if (formattedValue == null) {
            return Set.of();
        }

        return formattedValue
                .stream()
                .map(DomainAndUserSelectInputElementValue::toComparableKey)
                .collect(Collectors.toSet());
    }

    private static boolean isAllowedType(@Nullable String type) {
        return "orgUnit".equals(type) || "team".equals(type) || "user".equals(type);
    }

    private boolean isTypeEnabled(@Nullable String type) {
        if (!isAllowedType(type)) {
            return false;
        }

        if (allowedTypes == null) {
            return true;
        }

        return allowedTypes.contains(type);
    }

    @Nonnull
    private static String formatDisplayToken(@Nonnull DomainAndUserSelectInputElementValue token) {
        if ("orgUnit".equals(token.getType())) {
            return "Organisationseinheit (ID: " + token.getId() + ")";
        }

        if ("team".equals(token.getType())) {
            return "Team (ID: " + token.getId() + ")";
        }

        if ("user".equals(token.getType())) {
            return "Mitarbeiter:in (ID: " + token.getId() + ")";
        }

        return token.toComparableKey();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DomainAndUserSelectInputElement that = (DomainAndUserSelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(minItems, that.minItems)
                && Objects.equals(maxItems, that.maxItems)
                && Objects.equals(allowedTypes, that.allowedTypes)
                && Objects.equals(processAccessConstraint, that.processAccessConstraint);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(minItems);
        result = 31 * result + Objects.hashCode(maxItems);
        result = 31 * result + Objects.hashCode(allowedTypes);
        result = 31 * result + Objects.hashCode(processAccessConstraint);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DomainAndUserSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public Integer getMinItems() {
        return minItems;
    }

    public DomainAndUserSelectInputElement setMinItems(@Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    @Nullable
    public Integer getMaxItems() {
        return maxItems;
    }

    public DomainAndUserSelectInputElement setMaxItems(@Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    @Nullable
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public DomainAndUserSelectInputElement setAllowedTypes(@Nullable List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
        return this;
    }

    @Nullable
    public DomainAndUserSelectProcessAccessConstraint getProcessAccessConstraint() {
        return processAccessConstraint;
    }

    public DomainAndUserSelectInputElement setProcessAccessConstraint(@Nullable DomainAndUserSelectProcessAccessConstraint processAccessConstraint) {
        this.processAccessConstraint = processAccessConstraint;
        return this;
    }
}
