package de.aivot.GoverBackend.elements.models.elements.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

public class TabLayoutElement extends BaseElement implements LayoutElement<StepElement> {
    private List<StepElement> children = new LinkedList<>();

    public TabLayoutElement() {
        super(ElementType.TabLayout);
    }

    @Nonnull
    @Override
    public List<StepElement> getChildren() {
        return children;
    }

    @Nonnull
    @Override
    public LayoutElement<StepElement> setChildren(@Nullable List<StepElement> children) {
        this.children = children;
        return this;
    }
}
