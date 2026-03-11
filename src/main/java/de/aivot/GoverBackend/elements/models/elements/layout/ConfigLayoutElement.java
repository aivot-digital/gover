package de.aivot.GoverBackend.elements.models.elements.layout;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ConfigLayoutElement extends BaseElement implements LayoutElement<BaseFormElement> {
    private List<BaseFormElement> children = new LinkedList<>();

    public ConfigLayoutElement() {
        super(ElementType.ConfigLayout);
    }

    @Nonnull
    @Override
    public List<BaseFormElement> getChildren() {
        return children;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement setChildren(@Nullable List<BaseFormElement> children) {
        this.children = children;
        return this;
    }
}
