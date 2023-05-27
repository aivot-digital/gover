package de.aivot.GoverBackend.models.elements.form.layout;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public class GroupLayout extends BaseFormElement {
    private Collection<BaseFormElement> children;

    public GroupLayout(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        children = MapUtils.getCollection(values, "children", ElementResolver::resolve);
    }

    @Override
    public void validate(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (children != null) {
            for (var child : children) {
                child.patch(root, customerInput, idPrefix, scriptEngine);
                if (child.isVisible(root, customerInput, idPrefix, scriptEngine)) {
                    child.validate(root, customerInput, idPrefix, scriptEngine);
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (var child : children) {
                child.patch(root, customerInput, idPrefix, scriptEngine);
                if (child.isVisible(root, customerInput, idPrefix, scriptEngine)) {
                    rows.addAll(child.toPdfRows(root, customerInput, idPrefix, scriptEngine));
                }
            }
        }

        return rows;
    }

    public Optional<BaseFormElement> findChild(String id) {
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

    public Collection<BaseFormElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<BaseFormElement> children) {
        this.children = children;
    }

}
