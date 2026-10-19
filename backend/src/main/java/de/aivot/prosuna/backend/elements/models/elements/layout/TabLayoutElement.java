package de.aivot.prosuna.backend.elements.models.elements.layout;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TabLayoutElement extends BaseElement implements LayoutElement<GenericStepElement> {
    private List<GenericStepElement> children = new LinkedList<>();

    public TabLayoutElement() {
        super(ElementType.TabLayout);
    }

    @Nonnull
    @Override
    public List<GenericStepElement> getChildren() {
        return children;
    }

    @Nonnull
    @Override
    public LayoutElement<GenericStepElement> setChildren(@Nullable List<GenericStepElement> children) {
        this.children = children;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TabLayoutElement that = (TabLayoutElement) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children);
    }
}
