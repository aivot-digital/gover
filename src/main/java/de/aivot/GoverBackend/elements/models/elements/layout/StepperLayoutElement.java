package de.aivot.GoverBackend.elements.models.elements.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class StepperLayoutElement extends BaseElement implements LayoutElement<GenericStepElement> {
    private List<GenericStepElement> children = new LinkedList<>();

    public StepperLayoutElement() {
        super(ElementType.StepperLayout);
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
        StepperLayoutElement that = (StepperLayoutElement) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children);
    }
}
