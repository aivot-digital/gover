package de.aivot.GoverBackend.elements.models.steps;

import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.MapUtils;

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
    public List<BasePdfRowDto> toPdfRows(RootElement root, String idPrefix, FormState formState) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        if (children != null) {
            for (var child : children) {
                var isVisible = formState.visibilities().getOrDefault(child.getResolvedId(idPrefix), true);
                if (!isVisible) {
                    continue;
                }
                var resolvedChild = formState.overrides().getOrDefault(child.getResolvedId(idPrefix), child);
                rows.addAll(resolvedChild.toPdfRows(root, idPrefix, formState));
            }
        }

        if (rows.isEmpty()) {
            return new LinkedList<>();
        }

        rows.addFirst(new HeadlinePdfRowDto(
                title == null ? "Unbenannter Abschnitt" : title,
                2,
                this
        ));

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StepElement that = (StepElement) o;

        if (!Objects.equals(title, that.title)) return false;
        if (!Objects.equals(icon, that.icon)) return false;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
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
