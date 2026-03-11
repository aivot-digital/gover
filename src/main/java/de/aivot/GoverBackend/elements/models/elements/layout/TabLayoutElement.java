package de.aivot.GoverBackend.elements.models.elements.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

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
}
