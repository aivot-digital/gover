package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;

import java.util.Optional;

@Deprecated
public class ElementData {
    @Nonnull protected AuthoredElementValues authoredElementValues;
    @Nonnull protected ComputedElementStates computedElementStates;
    @Nonnull protected EffectiveElementValues effectiveElementValues;

    public ElementData(@Nonnull AuthoredElementValues authoredElementValues,
                       @Nonnull ComputedElementStates computedElementStates,
                       @Nonnull EffectiveElementValues effectiveElementValues) {
        this.authoredElementValues = authoredElementValues;
        this.computedElementStates = computedElementStates;
        this.effectiveElementValues = effectiveElementValues;
    }

    public Optional<ElementDataObject> getOpt(BaseElement forElement) {
        return Optional.of(mustGet(forElement));
    }

    @Nonnull
    public ElementDataObject mustGet(BaseElement forElement) {
        return new ElementDataObject(
                forElement,
                this
        );
    }

    public boolean hasAnyError() {
        return hasAnyErrorRecursive(computedElementStates);
    }

    private static boolean hasAnyErrorRecursive(ComputedElementStates computedElementStates) {
        return computedElementStates
                .values()
                .stream()
                .anyMatch(elementState -> elementState.getError() != null || (
                        elementState.getSubStates() != null &&
                                elementState.getSubStates().stream().anyMatch(ElementData::hasAnyErrorRecursive)
                ));
    }

    @Nonnull
    public AuthoredElementValues getAuthoredElementValues() {
        return authoredElementValues;
    }

    @Nonnull
    public ComputedElementStates getComputedElementStates() {
        return computedElementStates;
    }

    @Nonnull
    public EffectiveElementValues getEffectiveElementValues() {
        return effectiveElementValues;
    }
}
