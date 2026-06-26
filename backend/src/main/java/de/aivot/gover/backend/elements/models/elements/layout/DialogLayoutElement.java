package de.aivot.gover.backend.elements.models.elements.layout;

import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.LayoutElement;
import de.aivot.gover.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DialogLayoutElement extends BaseFormElement implements LayoutElement<BaseFormElement> {
    private List<BaseFormElement> children = new LinkedList<>();

    public DialogLayoutElement() {
        super(ElementType.DialogLayout);
    }

    @Nonnull
    @Override
    public List<BaseFormElement> getChildren() {
        return children;
    }

    @Nonnull
    @Override
    public LayoutElement<BaseFormElement> setChildren(@Nullable List<BaseFormElement> children) {
        this.children = children;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DialogLayoutElement that = (DialogLayoutElement) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children);
    }
}
