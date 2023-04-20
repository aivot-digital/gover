package de.aivot.GoverBackend.models.elements.form.layout;

import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    public void validate(Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        } else {
            if (minimumRequiredSets != null && value.size() < minimumRequiredSets) {
                throw new ValidationException(this, "Not enough items");
            }

            if (maximumSets != null && value.size() > maximumSets) {
                throw new ValidationException(this, "Too many items");
            }

            if (children != null) {
                for (var val : value) {
                    for (var child : children) {
                        child.validate(customerInput, getResolvedId(idPrefix) + "_" + val, scriptEngine);
                    }
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (value != null && !value.isEmpty()) {
            fields.add(new HeadlinePdfRowDto(getLabel(), 4));

            List<String> values = value.stream().toList();
            for (int i = 0; i < value.size(); i++) {
                String childId = values.get(i);

                String headline = headlineTemplate != null ? headlineTemplate.replace("#", "" + (i + 1)) : String.valueOf(i + 1);
                fields.add(new HeadlinePdfRowDto(headline, 5));

                for (var child : children) {
                    fields.addAll(child.toPdfRows(customerInput, getResolvedId(idPrefix) + "_" + childId, scriptEngine));
                }
            }
        }

        return fields;
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
