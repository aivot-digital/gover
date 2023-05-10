package de.aivot.GoverBackend.models.elements.steps;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StepElement extends BaseElement {
    private String title;
    private String icon;
    private Collection<BaseFormElement> children;

    public StepElement(Map<String, Object> values) {
        super(values);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        title = MapUtils.getString(values, "title");
        icon = MapUtils.getString(values, "icon");
        children = MapUtils.getCollection(values, "children", ElementResolver::resolve);
    }

    @Override
    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (children != null) {
            for (var child : children) {
                child.patch(customerInput, idPrefix, scriptEngine);
                if (child.isVisible(customerInput, idPrefix, scriptEngine)) {
                    child.validate(customerInput, idPrefix, scriptEngine);
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        rows.add(new HeadlinePdfRowDto(title == null ? "Unbenannter Abschnitt" : title, 1));

        if (children != null) {
            for (var child : children) {
                child.patch(customerInput, idPrefix, scriptEngine);
                if (child.isVisible(customerInput, idPrefix, scriptEngine)) {
                    rows.addAll(child.toPdfRows(customerInput, idPrefix, scriptEngine));
                }
            }
        }

        return rows;
    }

    // region Getters & Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Collection<BaseFormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<BaseFormElement> children) {
        this.children = children;
    }

    // endregion
}
