package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.content.Headline;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;

import java.util.Collection;
import java.util.HashMap;
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

    public record ElementWithIndent(int indent, BaseElement element) {
    }

    public static Collection<ElementWithIndent> flattenElementsWithContext(BaseElement current, int indent) {
        if (current == null) {
            return List.of();
        }

        Collection<? extends BaseElement> children = switch (current) {
            case RootElement rootElement -> rootElement.getChildren();
            case StepElement stepElement -> stepElement.getChildren();
            case GroupLayout groupLayout -> groupLayout.getChildren();
            case ReplicatingContainerLayout replicatingContainerLayout -> replicatingContainerLayout.getChildren();
            default -> List.of();
        };

        int indentModifier = current instanceof ReplicatingContainerLayout ? 1 : 0;

        var result = new LinkedList<ElementWithIndent>();
        result.add(new ElementWithIndent(indent, current));
        if (children != null) {
            if (current instanceof ReplicatingContainerLayout replicatingContainerLayout) {
                var min = replicatingContainerLayout.getMinimumRequiredSets();
                if (min == null) {
                    min = 5;
                }
                var max = replicatingContainerLayout.getMaximumSets();
                if (max == null) {
                    max = Math.min(min, 5);
                }
                for (int i = 0; i < Math.max(min, max); i++) {
                    var headline = new Headline(new HashMap<>());
                    headline.setContent(replicatingContainerLayout.getHeadlineTemplate().replaceAll("#", "" + (i + 1))); // TODO
                    headline.setSmall(true);
                    headline.setType(ElementType.Headline);
                    result.add(new ElementWithIndent(indent + indentModifier, headline));

                    children
                            .stream()
                            .map(child -> flattenElementsWithContext(child, indent + indentModifier))
                            .forEach(result::addAll);
                }
            } else {
                children
                        .stream()
                        .map(child -> flattenElementsWithContext(child, indent + indentModifier))
                        .forEach(result::addAll);
            }
        }
        return result;
    }
}
