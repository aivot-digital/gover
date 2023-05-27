package de.aivot.GoverBackend.models.elements.steps;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

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

        rows.add(new HeadlinePdfRowDto(title == null ? "Unbenannter Abschnitt" : title, 2));

        if (children != null) {
            for (var child : children) {
                child.patch(root, customerInput, idPrefix, scriptEngine);
                boolean childIsVisible = child.isVisible(root, customerInput, idPrefix, scriptEngine);
                if (childIsVisible) {
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
