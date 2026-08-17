package de.aivot.gover.backend.elements.models;

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

    public static DerivedRuntimeElementData empty() {
        return new DerivedRuntimeElementData()
                .setEffectiveValues(new EffectiveElementValues())
                .setElementStates(new ComputedElementStates());
    }

    // endregion

    // region Utilities

    /**
     * Put an error by its field.
     *
     * @param field  The field to set the error for.
     * @param format The error message template to set.
     * @param args   The arguments to format the error message template with.
     * @return The current derived runtime element data instance.
     */
    public DerivedRuntimeElementData putError(@Nonnull String field, @Nonnull String format, @Nonnull Object... args) {
        var message = String.format(format, args);
        return putError(field, message);
    }

    /**
     * Put an error by its field.
     *
     * @param field The field to set the error for.
     * @param error The error message to set.
     * @return The current derived runtime element data instance.
     */
    public DerivedRuntimeElementData putError(@Nonnull String field, @Nonnull String error) {
        var s = this
                .elementStates
                .computeIfAbsent(field, k -> new ComputedElementState());

        s.setError(error);

        return this;
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

    public boolean hasAnyError() {
        return hasAnyError(elementStates);
    }

    private static boolean hasAnyError(@Nonnull ComputedElementStates computedElementStates) {
        return computedElementStates
                .values()
                .stream()
                .anyMatch(elementState -> elementState.getError() != null || (
                        elementState.getSubStates() != null &&
                                elementState.getSubStates().stream().anyMatch(subState -> hasAnyError(subState.getStates()))
                ));
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
