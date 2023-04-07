package de.aivot.GoverBackend.models.elements.form.layout;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GroupLayout extends FormElement {
    private Collection<FormElement> children;

    public GroupLayout(BaseElement parent, Map<String, Object> data) {
        super(data);

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
    public Collection<FormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<FormElement> children) {
        this.children = children;
    }

    @Override
    public boolean isValid(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return children.stream().allMatch(c -> c.isValid(customerInput, idPrefix));
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (FormElement child : children) {
                rows.addAll(child.toPdfRows(customerInput, idPrefix));
            }
        }

        return rows;
    }
}
