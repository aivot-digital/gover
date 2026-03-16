package de.aivot.GoverBackend.elements.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Deprecated
public class ElementDataObject implements Serializable {
    private final BaseElement element;
    private final ElementData elementData;

    public ElementDataObject(BaseElement element, ElementData elementData) {
        this.element = element;
        this.elementData = elementData;
    }

    public ElementType getType() {
        return element.getType();
    }

    @Nullable
    public Object getInputValue() {
        return elementData
                .authoredElementValues
                .getOrDefault(element.getId(), null);
    }

    @Nonnull
    public Boolean getIsVisible() {
        return elementData
                .computedElementStates
                .getOrDefault(element.getId(), ComputedElementState.create())
                .getVisible();
    }

    @Nonnull
    public Boolean getIsPrefilled() {
        return false;
    }

    @Nonnull
    public Boolean getIsDirty() {
        return getInputValue() != null;
    }

    @Nullable
    public BaseElement getComputedOverride() {
        return elementData
                .computedElementStates
                .getOrDefault(element.getId(), ComputedElementState.create())
                .getOverride();
    }

    @Nonnull
    public BaseElement getComputedOverrideOrDefault(@Nonnull BaseElement element) {
        return getComputedOverride() != null ? getComputedOverride() : element;
    }

    @Nullable
    public Object getComputedValue() {
        return elementData
                .effectiveElementValues
                .getOrDefault(element.getId(), null);
    }

    @Nullable
    public List<String> getComputedErrors() {
        var err = elementData
                .computedElementStates
                .getOrDefault(element.getId(), ComputedElementState.create())
                .getError();
        return err != null ? List.of(err) : null;
    }

    @Nullable
    @JsonIgnore
    public Object getValue() {
        if (getIsDirty()) {
            return getInputValue();
        }
        return getInputValue() != null
                ? getInputValue()
                : elementData
                .effectiveElementValues
                .getOrDefault(element.getId(), null);
    }

    @JsonIgnore
    public Optional<Object> getOptionalValue() {
        var value = getValue();
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @JsonIgnore
    public <T> Optional<T> getOptionalValue(Class<T> clazz) {
        var value = getValue();
        if (value == null) {
            return Optional.empty();
        }
        if (clazz.isAssignableFrom(value.getClass())) {
            return Optional.of(clazz.cast(value));
        }
        return Optional.empty();
    }

    @Nullable
    public <T> T getValue(Class<T> clazz, T def) {
        return getOptionalValue(clazz).orElse(def);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getValue() == null;
    }

    @JsonIgnore
    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
