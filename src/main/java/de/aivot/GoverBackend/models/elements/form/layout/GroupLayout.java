package de.aivot.GoverBackend.models.elements.form.layout;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GroupLayout extends FormElement {
    private Collection<FormElement> children;

    public GroupLayout(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        children = MapUtils.getCollection(values, "children", ElementResolver::resolve);
    }

    @Override
    public void validate(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (children != null) {
            for (var child : children) {
                child.validate(customerInput, idPrefix, scriptEngine);
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (FormElement child : children) {
                rows.addAll(child.toPdfRows(customerInput, idPrefix, scriptEngine));
            }
        }

        return rows;
    }

    public Collection<FormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<FormElement> children) {
        this.children = children;
    }

}
