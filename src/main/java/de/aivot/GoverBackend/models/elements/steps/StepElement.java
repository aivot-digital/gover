package de.aivot.GoverBackend.models.elements.steps;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StepElement extends BaseElement {
    private String title;
    private String icon;
    private Collection<FormElement> children;

    public StepElement(Map<String, Object> data) {
        super(data);

        title = (String) data.get("title");
        icon = (String) data.get("icon");

        if (data.get("children") != null) {
            children = new LinkedList<>();
            for (Map<String, Object> childData : (Collection<Map<String, Object>>) data.get("children")) {
                if (childData != null) {
                    children.add(ElementResolver.resolve(this, childData));
                }
            }
        }
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

        rows.add(new HeadlinePdfRowDto(title == null ? "Unbenannter Abschnitt" : title, 1));

        if (children != null) {
            for (var child : children) {
                rows.addAll(child.toPdfRows(customerInput, idPrefix));
            }
        }

        return rows;
    }
}
