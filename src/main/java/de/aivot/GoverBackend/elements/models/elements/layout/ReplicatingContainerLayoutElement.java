package de.aivot.GoverBackend.elements.models.elements.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class ReplicatingContainerLayoutElement extends BaseInputElement<List<AuthoredElementValues>> implements LayoutElement<BaseFormElement> {
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

    @Nonnull
    private List<BaseFormElement> children = new LinkedList<>();

    public ReplicatingContainerLayoutElement() {
        super(ElementType.ReplicatingContainer);
    }

    @Override
    public List<AuthoredElementValues> formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        ObjectMapper om = ObjectMapperFactory.
                getInstance();

        List<AuthoredElementValues> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object itemObj : cValue) {
                var conv = om.convertValue(itemObj, AuthoredElementValues.class);
                res.add(conv);
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public void performValidation(List<AuthoredElementValues> value) throws ValidationException {
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
        Collection<AuthoredElementValues> listVal = formatValue(referencedValue);
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

        ReplicatingContainerLayoutElement that = (ReplicatingContainerLayoutElement) o;
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

    public ReplicatingContainerLayoutElement setMinimumRequiredSets(@Nullable Integer minimumRequiredSets) {
        this.minimumRequiredSets = minimumRequiredSets;
        return this;
    }

    @Nullable
    public Integer getMaximumSets() {
        return maximumSets;
    }

    public ReplicatingContainerLayoutElement setMaximumSets(@Nullable Integer maximumSets) {
        this.maximumSets = maximumSets;
        return this;
    }

    @Nullable
    public String getHeadlineTemplate() {
        return headlineTemplate;
    }

    public ReplicatingContainerLayoutElement setHeadlineTemplate(@Nullable String headlineTemplate) {
        this.headlineTemplate = headlineTemplate;
        return this;
    }

    @Nullable
    public String getAddLabel() {
        return addLabel;
    }

    public ReplicatingContainerLayoutElement setAddLabel(@Nullable String addLabel) {
        this.addLabel = addLabel;
        return this;
    }

    @Nullable
    public String getRemoveLabel() {
        return removeLabel;
    }

    public ReplicatingContainerLayoutElement setRemoveLabel(@Nullable String removeLabel) {
        this.removeLabel = removeLabel;
        return this;
    }

    @Override
    @Nonnull
    public List<BaseFormElement> getChildren() {
        if (children == null) {
            children = new LinkedList<>();
        }
        return children;
    }

    @Nonnull
    @Override
    public ReplicatingContainerLayoutElement setChildren(@Nullable List<BaseFormElement> children) {
        if (children == null) {
            children = new LinkedList<>();
        }
        this.children = children;
        return this;
    }

    // endregion
}
