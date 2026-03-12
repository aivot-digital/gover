package de.aivot.GoverBackend.elements.models;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

public class DerivedRuntimeElementData implements Serializable {
    @Nonnull
    private EffectiveElementValues effectiveValues = new EffectiveElementValues();

    @Nonnull
    private ComputedElementStates elementStates = new ComputedElementStates();

    // region Constructors

    public DerivedRuntimeElementData() {
    }

    public DerivedRuntimeElementData(@Nonnull EffectiveElementValues effectiveValues,
                                     @Nonnull ComputedElementStates elementStates) {
        this.effectiveValues = effectiveValues;
        this.elementStates = elementStates;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DerivedRuntimeElementData that = (DerivedRuntimeElementData) o;
        return Objects.equals(effectiveValues, that.effectiveValues) && Objects.equals(elementStates, that.elementStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectiveValues, elementStates);
    }
    
    // endregion

    // region Getters & Setters

    @Nonnull
    public EffectiveElementValues getEffectiveValues() {
        return effectiveValues;
    }

    public DerivedRuntimeElementData setEffectiveValues(@Nonnull EffectiveElementValues effectiveValues) {
        this.effectiveValues = effectiveValues;
        return this;
    }

    @Nonnull
    public ComputedElementStates getElementStates() {
        return elementStates;
    }

    public DerivedRuntimeElementData setElementStates(@Nonnull ComputedElementStates elementStates) {
        this.elementStates = elementStates;
        return this;
    }

    // endregion
}
