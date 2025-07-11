package de.aivot.GoverBackend.elements.models.elements.form.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class ReplicatingContainerLayout extends BaseInputElement<List<ElementData>> implements ElementWithChildren<BaseFormElement> {
    @Nullable
    private Integer minimumRequiredSets;
    @Nullable
    private Integer maximumSets;
    @Nullable
    private String headlineTemplate;
    @Nullable
    private String addLabel;
    @Nullable
    private String removeLabel;
    @Nullable
    private List<BaseFormElement> children;

    public ReplicatingContainerLayout() {
        super(ElementType.ReplicatingContainer);
    }

    @Override
    public List<ElementData> formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        List<ElementData> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            var om = new ObjectMapper();

            for (Object itemObj : cValue) {
                switch (itemObj) {
                    case null -> {
                        // Skip null values
                        continue;
                    }
                    case ElementData elementData -> {
                        // If the item is already an ElementData, add it directly
                        res.add(elementData);
                    }
                    case Map<?, ?> mapValue -> {
                        try {
                            var item = om.convertValue(mapValue, ElementData.class);
                            res.add(item);
                        } catch (IllegalArgumentException ex) {
                            // If conversion fails, skip this item
                            continue;
                        }
                    }
                    default -> {
                        continue;
                    }
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<ElementData> value) {
        return ""; // TODO: Implement a meaningful display value for the replicating container layout
    }

    @Override
    public void performValidation(List<ElementData> value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        } else {
            var minSets = Boolean.TRUE.equals(getRequired()) ? (minimumRequiredSets != null ? minimumRequiredSets : 1) : 0;
            if (value.size() < minSets) {
                throw new ValidationException(this, "Nicht genug Datensätze.");
            }

            var hasMaximum = maximumSets != null && maximumSets > 0;
            if (hasMaximum && value.size() > maximumSets) {
                throw new ValidationException(this, "Zu viele Datensätze.");
            }
        }
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        Collection<ElementData> listVal = formatValue(referencedValue);
        int listValInt = listVal != null ? listVal.size() : 0;

        var comparedValueInt = switch (comparedValue) {
            case Integer i -> i;
            case String s -> {
                try {
                    yield Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                    yield 0;
                }
            }
            case BigDecimal bigDecimal -> bigDecimal.intValue();
            case Double v -> v.intValue();
            case Long l -> l.intValue();
            case Float v -> v.intValue();
            case Collection<?> c -> c.size();
            case null, default -> 0;
        };

        return switch (operator) {
            case Empty -> listVal == null || listVal.isEmpty();
            case NotEmpty -> listVal != null && !listVal.isEmpty();

            case ReplicatingListLengthEquals -> listValInt == comparedValueInt;
            case ReplicatingListLengthNotEquals -> listValInt != comparedValueInt;

            case ReplicatingListLengthLessThan -> listValInt < comparedValueInt;
            case ReplicatingListLengthLessThanOrEqual -> listValInt <= comparedValueInt;
            case ReplicatingListLengthGreaterThan -> listValInt > comparedValueInt;
            case ReplicatingListLengthGreaterThanOrEqual -> listValInt >= comparedValueInt;

            default -> false;
        };
    }

    public String getResolvedHeadline(@Nullable Integer index) {
        if (index == null || index < 0) {
            return "";
        }

        if (headlineTemplate == null) {
            return index.toString();
        }

        return headlineTemplate
                .replaceAll("#", "" + (index + 1));
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ReplicatingContainerLayout that = (ReplicatingContainerLayout) o;
        return Objects.equals(minimumRequiredSets, that.minimumRequiredSets) && Objects.equals(maximumSets, that.maximumSets) && Objects.equals(headlineTemplate, that.headlineTemplate) && Objects.equals(addLabel, that.addLabel) && Objects.equals(removeLabel, that.removeLabel) && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(minimumRequiredSets);
        result = 31 * result + Objects.hashCode(maximumSets);
        result = 31 * result + Objects.hashCode(headlineTemplate);
        result = 31 * result + Objects.hashCode(addLabel);
        result = 31 * result + Objects.hashCode(removeLabel);
        result = 31 * result + Objects.hashCode(children);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public Integer getMinimumRequiredSets() {
        return minimumRequiredSets;
    }

    public ReplicatingContainerLayout setMinimumRequiredSets(@Nullable Integer minimumRequiredSets) {
        this.minimumRequiredSets = minimumRequiredSets;
        return this;
    }

    @Nullable
    public Integer getMaximumSets() {
        return maximumSets;
    }

    public ReplicatingContainerLayout setMaximumSets(@Nullable Integer maximumSets) {
        this.maximumSets = maximumSets;
        return this;
    }

    @Nullable
    public String getHeadlineTemplate() {
        return headlineTemplate;
    }

    public ReplicatingContainerLayout setHeadlineTemplate(@Nullable String headlineTemplate) {
        this.headlineTemplate = headlineTemplate;
        return this;
    }

    @Nullable
    public String getAddLabel() {
        return addLabel;
    }

    public ReplicatingContainerLayout setAddLabel(@Nullable String addLabel) {
        this.addLabel = addLabel;
        return this;
    }

    @Nullable
    public String getRemoveLabel() {
        return removeLabel;
    }

    public ReplicatingContainerLayout setRemoveLabel(@Nullable String removeLabel) {
        this.removeLabel = removeLabel;
        return this;
    }

    @Override
    @Nullable
    public List<BaseFormElement> getChildren() {
        return children;
    }

    @Override
    public ReplicatingContainerLayout setChildren(@Nullable List<BaseFormElement> children) {
        this.children = children;
        return this;
    }

    // endregion
}
