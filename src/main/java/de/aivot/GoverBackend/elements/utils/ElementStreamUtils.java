package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ElementStreamUtils {
    public static void applyAction(BaseElement element, Consumer<BaseElement> action) {
        action.accept(element);

        if (element instanceof LayoutElement<?> layoutElement) {
            for (BaseElement child : layoutElement.getChildren()) {
                applyAction(child, action);
            }
        }
    }

    public static void applyActionWithParents(BaseElement element, BiConsumer<List<BaseElement>, BaseElement> action) {
        applyActionWithParents(new LinkedList<>(), element, action);
    }

    private static void applyActionWithParents(List<BaseElement> parents, BaseElement element, BiConsumer<List<BaseElement>, BaseElement> action) {
        action.accept(parents, element);

        if (element instanceof LayoutElement<?> layoutElement) {
            var newParents = new LinkedList<>(parents);
            newParents.add(element);

            for (BaseElement child : layoutElement.getChildren()) {
                applyActionWithParents(newParents, child, action);
            }
        }
    }

    public static void applyAction(BaseElement element, ComputedElementStates states, BiConsumer<BaseElement, ComputedElementState> action) {
        var state = states.getOrDefault(element.getId(), new ComputedElementState());

        action.accept(element, state);

        if (element instanceof ReplicatingContainerLayoutElement replicatingContainerLayoutElement) {
            var substates = state.getSubStates();

            if (substates != null) {
                for (var substate : substates) {
                    for (BaseElement child : replicatingContainerLayoutElement.getChildren()) {
                        applyAction(child, substate, action);
                    }
                }
            }
        } else if (element instanceof LayoutElement<?> layoutElement) {
            for (BaseElement child : layoutElement.getChildren()) {
                applyAction(child, states, action);
            }
        }
    }
}
