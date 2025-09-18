package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class StepElement extends BaseElement implements ElementWithChildren<BaseFormElement> {
    @Nullable
    private String title;
    @Nullable
    private String icon;
    @Nullable
    private List<BaseFormElement> children = new LinkedList<>();

    public StepElement() {
        super(ElementType.Step);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StepElement that = (StepElement) o;
        return Objects.equals(title, that.title) && Objects.equals(icon, that.icon) && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + Objects.hashCode(icon);
        result = 31 * result + Objects.hashCode(children);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getTitle() {
        return title;
    }

    public StepElement setTitle(@Nullable String title) {
        this.title = title;
        return this;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public StepElement setIcon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    @Override
    @Nonnull
    public List<BaseFormElement> getChildren() {
        if (children == null) {
            children = new LinkedList<>();
        }
        return children;
    }

    @Nonnull
    @Override
    public StepElement setChildren(@Nullable List<BaseFormElement> children) {
        if (children == null) {
            children = new LinkedList<>();
        }
        this.children = children;
        return this;
    }

    // endregion
}
