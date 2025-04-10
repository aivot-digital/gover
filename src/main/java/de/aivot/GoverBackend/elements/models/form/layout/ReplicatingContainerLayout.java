package de.aivot.GoverBackend.elements.models.form.layout;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;

public class ReplicatingContainerLayout extends BaseInputElement<Collection<String>> {
    private Integer minimumRequiredSets;
    private Integer maximumSets;
    private String headlineTemplate;
    private String addLabel;
    private String removeLabel;
    private Collection<BaseFormElement> children;

    public ReplicatingContainerLayout(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        minimumRequiredSets = MapUtils.getInteger(values, "minimumRequiredSets");
        maximumSets = MapUtils.getInteger(values, "maximumSets");
        headlineTemplate = MapUtils.getString(values, "headlineTemplate");
        addLabel = MapUtils.getString(values, "addLabel");
        removeLabel = MapUtils.getString(values, "removeLabel");

        children = MapUtils.getCollection(values, "children", ElementResolver::resolve);
    }

    @Override
    public Collection<String> formatValue(Object value) {
        Collection<String> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object item : cValue) {
                if (item instanceof String sValue) {
                    res.add(sValue);
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(Collection<String> value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        } else {
            var minSets = Boolean.TRUE.equals(getRequired()) ? (minimumRequiredSets != null ? minimumRequiredSets : 1) : 0;
            if (value.size() < minSets) {
                throw new ValidationException(this, "Nicht genug Datensätze.");
            }

            if (maximumSets != null && value.size() > maximumSets) {
                throw new ValidationException(this, "Zu viele Datensätze.");
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Collection<String> value, String idPrefix, FormState formState) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new HeadlinePdfRowDto(getLabel(), 4,
                this));

        if (value != null && !value.isEmpty()) {
            List<String> values = value.stream().toList();
            for (int i = 0; i < value.size(); i++) {
                String val = values.get(i);

                String headline = headlineTemplate != null ? headlineTemplate.replace("#", "" + (i + 1)) : String.valueOf(i + 1);
                fields.add(new HeadlinePdfRowDto(headline, 5,
                        this));

                for (var child : children) {
                    var isVisible = formState.visibilities().getOrDefault(child.getResolvedId(idPrefix), true);
                    if (!isVisible) {
                        continue;
                    }
                    String childId = getResolvedId(idPrefix) + "_" + val;
                    var resolvedChild = formState.overrides().getOrDefault(childId, child);
                    fields.addAll(resolvedChild.toPdfRows(root, childId, formState));
                }
            }
        } else {
            fields.add(new ValuePdfRowDto("", "Keine Angabe", this));
        }

        return fields;
    }

    @Override
    public Optional<BaseFormElement> findChild(@Nonnull String id) {
        Optional<BaseFormElement> matchingChild = children
                .stream()
                .filter(s -> s.matches(id))
                .findFirst();

        if (matchingChild.isPresent()) {
            return matchingChild;
        }

        return children
                .stream()
                .map(c -> {
                    Optional<BaseFormElement> res = Optional.empty();
                    if (c instanceof GroupLayout groupLayout) {
                        res = groupLayout.findChild(id);
                    } else if (c instanceof ReplicatingContainerLayout replicatingContainerLayout) {
                        res = replicatingContainerLayout.findChild(id);
                    }
                    return res;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        Collection<String> listVal = formatValue(referencedValue);
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
            case Double v ->  v.intValue();
            case Long l -> l.intValue();
            case Float v ->v.intValue();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ReplicatingContainerLayout that = (ReplicatingContainerLayout) o;

        if (!Objects.equals(minimumRequiredSets, that.minimumRequiredSets))
            return false;
        if (!Objects.equals(maximumSets, that.maximumSets)) return false;
        if (!Objects.equals(headlineTemplate, that.headlineTemplate))
            return false;
        if (!Objects.equals(addLabel, that.addLabel)) return false;
        if (!Objects.equals(removeLabel, that.removeLabel)) return false;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (minimumRequiredSets != null ? minimumRequiredSets.hashCode() : 0);
        result = 31 * result + (maximumSets != null ? maximumSets.hashCode() : 0);
        result = 31 * result + (headlineTemplate != null ? headlineTemplate.hashCode() : 0);
        result = 31 * result + (addLabel != null ? addLabel.hashCode() : 0);
        result = 31 * result + (removeLabel != null ? removeLabel.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }


    public String getResolvedHeadline(Integer index) {
        return getHeadlineTemplate().replaceAll("#", "" + (index + 1));
    }

//region Getters & Setters

    public Integer getMinimumRequiredSets() {
        return minimumRequiredSets;
    }

    public void setMinimumRequiredSets(Integer minimumRequiredSets) {
        this.minimumRequiredSets = minimumRequiredSets;
    }

    public Integer getMaximumSets() {
        return maximumSets;
    }

    public void setMaximumSets(Integer maximumSets) {
        this.maximumSets = maximumSets;
    }

    public String getHeadlineTemplate() {
        return headlineTemplate;
    }

    public void setHeadlineTemplate(String headlineTemplate) {
        this.headlineTemplate = headlineTemplate;
    }

    public String getAddLabel() {
        return addLabel;
    }

    public void setAddLabel(String addLabel) {
        this.addLabel = addLabel;
    }

    public String getRemoveLabel() {
        return removeLabel;
    }

    public void setRemoveLabel(String removeLabel) {
        this.removeLabel = removeLabel;
    }

    public Collection<BaseFormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<BaseFormElement> children) {
        this.children = children;
    }

    //endregion
}
