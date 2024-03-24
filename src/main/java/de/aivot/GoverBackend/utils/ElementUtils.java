package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.models.elements.steps.StepElement;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ElementUtils {
    public static Collection<BaseElement> flattenElements(BaseElement current) {
        Collection<? extends BaseElement> children;
        if (current instanceof RootElement rootElement) {
            children = rootElement.getChildren();
        } else if (current instanceof StepElement stepElement) {
            children = stepElement.getChildren();
        } else if (current instanceof GroupLayout groupLayout) {
            children = groupLayout.getChildren();
        } else if (current instanceof ReplicatingContainerLayout replicatingContainerLayout) {
            children = replicatingContainerLayout.getChildren();
        } else {
            return List.of(current);
        }

        var result = new LinkedList<BaseElement>();
        result.add(current);
        if (children != null) {
            children
                    .stream()
                    .map(ElementUtils::flattenElements)
                    .forEach(result::addAll);
        }
        return result;
    }
}
