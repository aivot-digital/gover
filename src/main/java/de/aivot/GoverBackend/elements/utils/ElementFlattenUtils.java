package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ElementFlattenUtils {
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
                    .map(ElementFlattenUtils::flattenElements)
                    .forEach(result::addAll);
        }
        return result;
    }

    public static List<ResolvedElement> resolveElements(
            @Nonnull BaseElement root,
            @Nonnull Map<String, Object> customerInput
    ) {
        return resolveElements(
                root,
                null,
                new LinkedList<>(),
                customerInput
        );
    }

    private static List<ResolvedElement> resolveElements(
            @Nonnull BaseElement currentElement,
            @Nullable String idPrefix,
            @Nonnull List<BaseElement> parents,
            @Nonnull Map<String, Object> customerInput
    ) {
        var result = new LinkedList<ResolvedElement>();

        result.add(new ResolvedElement(
                currentElement.getResolvedId(idPrefix),
                currentElement,
                List.copyOf(parents)
        ));

        var currentParents = new LinkedList<>(parents);
        currentParents.add(currentElement);

        switch (currentElement) {
            case RootElement rootElement:
                result.addAll(resolveElements(rootElement.getIntroductionStep(), idPrefix, currentParents, customerInput));
                result.addAll(resolveElements(rootElement.getSummaryStep(), idPrefix, currentParents, customerInput));
                result.addAll(resolveElements(rootElement.getSubmitStep(), idPrefix, currentParents, customerInput));
                for (var step : rootElement.getChildren()) {
                    result.addAll(resolveElements(step, idPrefix, currentParents, customerInput));
                }
                break;
            case StepElement stepElement:
                for (var step : stepElement.getChildren()) {
                    result.addAll(resolveElements(step, idPrefix, currentParents, customerInput));
                }
                break;
            case GroupLayout groupLayout:
                for (var child : groupLayout.getChildren()) {
                    result.addAll(resolveElements(child, idPrefix, currentParents, customerInput));
                }
                break;
            case ReplicatingContainerLayout replicatingContainerLayout:
                var values = customerInput.get(replicatingContainerLayout.getResolvedId(idPrefix));
                if (values instanceof Collection<?> collection) {
                    for (var value : collection) {
                        if (value instanceof String sValue) {
                            String childIdPrefix = replicatingContainerLayout.getResolvedId(idPrefix) + "_" + sValue;

                            for (var child : replicatingContainerLayout.getChildren()) {
                                result.addAll(resolveElements(child, childIdPrefix, currentParents, customerInput));
                            }
                        }
                    }
                }
            default:
                break;
        }

        return result;
    }

    public record ResolvedElement(
            String resolvedId,
            BaseElement element,
            List<BaseElement> parents
    ) {
    }
}
