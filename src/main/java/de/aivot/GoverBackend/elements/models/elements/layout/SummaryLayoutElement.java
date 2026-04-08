package de.aivot.GoverBackend.elements.models.elements.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SummaryLayoutElement extends BaseFormElement implements LayoutElement<BaseFormElement> {
    private List<BaseFormElement> children = new LinkedList<>();

    public SummaryLayoutElement() {
        super(ElementType.SummaryLayout);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SummaryLayoutElement that = (SummaryLayoutElement) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children);
    }

    @Nonnull
    @Override
    public List<BaseFormElement> getChildren() {
        return children;
    }

    @Nonnull
    @Override
    public SummaryLayoutElement setChildren(@Nullable List<BaseFormElement> children) {
        this.children = children;
        return this;
    }
}
