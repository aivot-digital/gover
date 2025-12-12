package de.aivot.GoverBackend.elements.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.aivot.GoverBackend.core.converters.ElementDataObjectDeserializer;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonDeserialize(using = ElementDataObjectDeserializer.class)
public class ElementDataObject implements Serializable {
    @JsonProperty("$type")
    private ElementType type;
    @Nullable
    private Object inputValue;
    @Nullable
    private Object previousInputValue;
    @Nullable
    private Boolean isVisible;
    @Nullable
    private Boolean isPrefilled;
    @Nullable
    private Boolean isDirty;
    @Nullable
    private BaseElement computedOverride;
    @Nullable
    private Object computedValue;
    @Nullable
    private List<String> computedErrors;

    public ElementDataObject() {

    }

    public ElementDataObject(BaseElement element) {
        this.type = element.getType();
    }

    public ElementDataObject(ElementType elementType) {
        this.type = elementType;
    }

    public ElementType getType() {
        return type;
    }

    public ElementDataObject setType(ElementType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public Object getInputValue() {
        return inputValue;
    }

    public ElementDataObject setInputValue(@Nullable Object inputValue) {
        this.inputValue = inputValue;
        return this;
    }

    @Nonnull
    public Boolean getIsVisible() {
        return isVisible != null ? isVisible : true;
    }

    public ElementDataObject setIsVisible(@Nullable Boolean visible) {
        isVisible = visible;
        return this;
    }

    @Nonnull
    public Boolean getIsPrefilled() {
        return isPrefilled != null ? isPrefilled : false;
    }

    public ElementDataObject setIsPrefilled(@Nullable Boolean prefilled) {
        isPrefilled = prefilled;
        return this;
    }

    @Nonnull
    public Boolean getIsDirty() {
        return isDirty != null ? isDirty : false;
    }

    public ElementDataObject setIsDirty(@Nullable Boolean dirty) {
        isDirty = dirty;
        return this;
    }

    @Nullable
    public BaseElement getComputedOverride() {
        return computedOverride;
    }

    @Nonnull
    public BaseElement getComputedOverrideOrDefault(@Nonnull BaseElement element) {
        return Objects.requireNonNullElse(computedOverride, element);
    }

    public ElementDataObject setComputedOverride(@Nullable BaseElement computedOverride) {
        this.computedOverride = computedOverride;
        return this;
    }

    @Nullable
    public Object getComputedValue() {
        return computedValue;
    }

    public ElementDataObject setComputedValue(@Nullable Object computedValue) {
        this.computedValue = computedValue;
        return this;
    }

    @Nullable
    public List<String> getComputedErrors() {
        return computedErrors;
    }

    public ElementDataObject setComputedErrors(@Nullable List<String> computedError) {
        this.computedErrors = computedError;
        return this;
    }

    public ElementDataObject setComputedError(@Nullable String computedError) {
        this.computedErrors = computedError == null ? null : List.of(computedError);
        return this;
    }

    public ElementDataObject addComputedError(@Nullable String computedError) {
        if (this.computedErrors == null) {
            this.computedErrors = new LinkedList<>();
        }
        this.computedErrors.add(computedError);
        return this;
    }

    @Nullable
    @JsonIgnore
    public Object getValue() {
        if (isDirty != null && isDirty) {
            return inputValue;
        }
        return inputValue != null ? inputValue : computedValue;
    }

    @JsonIgnore
    public Optional<Object> getOptionalValue() {
        var value = getValue();
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getValue() == null;
    }

    @JsonIgnore
    public boolean isNotEmpty() {
        return !isEmpty();
    }


    @Nullable
    public Object getPreviousInputValue() {
        return previousInputValue;
    }

    public ElementDataObject setPreviousInputValue(@Nullable Object previousInputValue) {
        this.previousInputValue = previousInputValue;
        return this;
    }
}
