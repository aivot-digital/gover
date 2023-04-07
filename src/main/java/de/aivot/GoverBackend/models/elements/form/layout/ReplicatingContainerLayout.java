package de.aivot.GoverBackend.models.elements.form.layout;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;

import java.util.*;

public class ReplicatingContainerLayout extends InputElement<String[]> {
    private Integer minimumRequiredSets;
    private Integer maximumSets;
    private String headlineTemplate;
    private String addLabel;
    private String removeLabel;
    private Collection<FormElement> children;

    public ReplicatingContainerLayout(BaseElement parent, Map<String, Object> data) {
        super(data);

        minimumRequiredSets = (Integer) data.get("minimumRequiredSets");
        maximumSets = (Integer) data.get("maximumSets");
        headlineTemplate = (String) data.get("headlineTemplate");
        addLabel = (String) data.get("addLabel");
        removeLabel = (String) data.get("removeLabel");


        Collection<Map<String, Object>> childDataCollection = (Collection<Map<String, Object>>) data.get("children");
        if (childDataCollection != null) {
            children = new LinkedList<>();
            for (Map<String, Object> childData : childDataCollection) {
                if (childData != null) {
                    children.add(ElementResolver.resolve(this, childData));
                }
            }
        }
    }

    @Nullable
    public Integer getMinimumRequiredSets() {
        return minimumRequiredSets;
    }

    public void setMinimumRequiredSets(Integer minimumRequiredSets) {
        this.minimumRequiredSets = minimumRequiredSets;
    }

    @Nullable
    public Integer getMaximumSets() {
        return maximumSets;
    }

    public void setMaximumSets(Integer maximumSets) {
        this.maximumSets = maximumSets;
    }

    @Nullable
    public String getHeadlineTemplate() {
        return headlineTemplate;
    }

    public void setHeadlineTemplate(String headlineTemplate) {
        this.headlineTemplate = headlineTemplate;
    }

    @Nullable
    public String getAddLabel() {
        return addLabel;
    }

    public void setAddLabel(String addLabel) {
        this.addLabel = addLabel;
    }

    @Nullable
    public String getRemoveLabel() {
        return removeLabel;
    }

    public void setRemoveLabel(String removeLabel) {
        this.removeLabel = removeLabel;
    }

    @Nullable
    public Collection<FormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<FormElement> children) {
        this.children = children;
    }

    @Override
    public boolean isValid(String[] value, String idPrefix) {
        // TODO
        return false;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(String[] value, String idPrefix) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        /* TODO
        List<String> childIds = (List<String>) value.get(id);

        if (childIds != null && !childIds.isEmpty()) {
            fields.add(new HeadlinePdfRowDto((String) containerElement.get("label"), 3));

            for (int i = 0; i < childIds.size(); i++) {
                String childId = childIds.get(i);
                String headlineTemplate = (String) containerElement.get("headlineTemplate");
                headlineTemplate = headlineTemplate.replace("#", "" + (i + 1));
                fields.add(new HeadlinePdfRowDto(headlineTemplate, 4));

                for (Map<String, Object> childElement : children) {
                    fields.addAll(processElement(childElement, id + "_" + childId));
                }
            }
        }
         */

        return fields;
    }
}
