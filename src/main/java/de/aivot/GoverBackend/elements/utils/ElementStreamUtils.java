package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;

import java.util.function.Consumer;

public class ElementStreamUtils {
    public static void applyAction(BaseElement element, Consumer<BaseElement> action) {
        action.accept(element);

        switch (element) {
            case FormLayoutElement rootElement:
                for (BaseElement child : rootElement.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case GenericStepElement stepElement:
                for (BaseElement child : stepElement.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case GroupLayoutElement groupLayout:
                for (BaseElement child : groupLayout.getChildren()) {
                    applyAction(child, action);
                }
                break;
            case ReplicatingContainerLayoutElement replicatingContainerLayout:
                for (BaseElement child : replicatingContainerLayout.getChildren()) {
                    applyAction(child, action);
                }
                break;
            default:
                break;
        }
    }
}
