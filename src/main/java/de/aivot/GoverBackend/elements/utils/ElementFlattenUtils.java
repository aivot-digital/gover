package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ElementFlattenUtils {
    public static Collection<BaseElement> flattenElements(BaseElement current) {
        Collection<? extends BaseElement> children;

        if (current instanceof FormLayoutElement rootElement) {
            children = rootElement.getChildren();
        } else if (current instanceof GenericStepElement stepElement) {
            children = stepElement.getChildren();
        } else if (current instanceof GroupLayoutElement groupLayout) {
            children = groupLayout.getChildren();
        } else if (current instanceof ReplicatingContainerLayoutElement replicatingContainerLayout) {
            children = replicatingContainerLayout.getChildren();
        } else {
            return List.of(current);
        }

        var result = new LinkedList<BaseElement>();
        result.add(current);
        if (children != null) {
            children
                    .stream()
                    .map(ElementFlattenUtils::flattenElements)
                    .forEach(result::addAll);
        }
        return result;
    }
}
