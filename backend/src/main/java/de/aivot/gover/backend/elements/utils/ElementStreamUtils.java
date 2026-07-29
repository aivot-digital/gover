package de.aivot.gover.backend.elements.utils;

import de.aivot.gover.backend.elements.models.ComputedElementState;
import de.aivot.gover.backend.elements.models.ComputedElementStates;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.LayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static BaseElement mapAction(BaseElement element, Function<BaseElement, BaseElement> action) {
        var mapped = action.apply(element);

        if (mapped != element) {
            return mapped;
        }

        if (mapped instanceof LayoutElement<?> mappedLayoutElement && element instanceof LayoutElement<?> originalLayoutElement) {
            mapLayoutChildren(mappedLayoutElement, originalLayoutElement, action);
        }

        return mapped;
    }

    private static <T extends BaseElement, S extends BaseElement> void mapLayoutChildren(LayoutElement<S> layoutElement,
                                                                                         LayoutElement<T> originalLayoutElement,
                                                                                         Function<BaseElement, BaseElement> action) {
        List<S> mappedChildren = new LinkedList<>();
        for (T child : originalLayoutElement.getChildren()) {
            BaseElement mappedChild = mapAction(child, action);

            S castedMappedChild;
            try {
                castedMappedChild = (S) mappedChild;
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Mapped child is not of the expected type: " + mappedChild.getClass().getName(), e);
            }

            mappedChildren.add(castedMappedChild);
        }
        layoutElement.setChildren(mappedChildren);
    }
}
