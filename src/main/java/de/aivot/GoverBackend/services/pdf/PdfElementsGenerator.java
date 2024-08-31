package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.models.elements.form.content.Headline;
import de.aivot.GoverBackend.models.elements.form.content.RichText;
import de.aivot.GoverBackend.models.elements.form.input.TableField;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.models.elements.steps.StepElement;
import de.aivot.GoverBackend.services.ScriptService;

import javax.script.ScriptEngine;
import java.util.*;

public class PdfElementsGenerator {

    public static List<PdfElement> generatePdfElements(RootElement rootElement, Optional<Map<String, Object>> customerInput) {
        var scriptEngine = ScriptService.getEngine();
        return generatePdfElements(
                rootElement,
                Optional.empty(),
                rootElement,
                0,
                customerInput,
                scriptEngine
        );
    }

    private static List<PdfElement> generatePdfElements(RootElement rootElement, Optional<String> idPrefix, BaseElement currentElement, int currentIndent, Optional<Map<String, Object>> customerInput, ScriptEngine scriptEngine) {
        if (currentElement == null) {
            return List.of();
        }

        if (customerInput.isPresent()) {
            currentElement.patch(idPrefix.orElse(null), rootElement, customerInput.get(), scriptEngine);
        }

        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            if (Boolean.TRUE.equals(baseInputElement.getTechnical())) {
                return List.of();
            }

            if (customerInput.isEmpty() && Boolean.TRUE.equals(baseInputElement.getDisabled())) {
                return List.of();
            }
        }

        if (customerInput.isPresent() && !currentElement.isVisible(idPrefix.orElse(null), rootElement, customerInput.get(), scriptEngine)) {
            return List.of();
        }

        Collection<? extends BaseElement> children = switch (currentElement) {
            case RootElement typedElement -> typedElement.getChildren();
            case StepElement typedElement -> typedElement.getChildren();
            case GroupLayout typedElement -> typedElement.getChildren();
            case ReplicatingContainerLayout typedElement -> typedElement.getChildren();
            default -> List.of();
        };

        List<PdfElement> resultList = new LinkedList<>();

        Object value = null;
        if (customerInput.isPresent() && currentElement instanceof BaseInputElement<?> inputElement) {
            Optional<?> computedValue = inputElement.getComputedValue(rootElement, customerInput.get(), idPrefix.orElse(null), scriptEngine);
            Object rawValue = computedValue.isPresent() ? computedValue.get() : customerInput.get().get(currentElement.getResolvedId(idPrefix.orElse(null)));
            value = inputElement.formatValue(rawValue);
        }

        resultList.add(new PdfElement(currentElement.getType(), currentElement, currentIndent, value));

        if (children != null) {
            if (currentElement instanceof ReplicatingContainerLayout replicatingContainerLayout) {
                var newIndent = currentIndent + 1;

                Collection<String> setsToRender;
                if (value instanceof Collection<?> cValue) {
                    setsToRender = (Collection<String>) cValue;
                } else {
                    if (customerInput.isPresent()) {
                        setsToRender = new LinkedList<>();
                    } else {
                        var min = replicatingContainerLayout.getMinimumRequiredSets();
                        var max = replicatingContainerLayout.getMaximumSets();
                        var amount = 0;
                        if (min == null && max == null) {
                            amount = 5;
                        } else if (max != null && min == null) {
                            amount = max;
                        } else if (max == null && min != null) {
                            amount = min;
                        } else {
                            amount = Math.max(max, min);
                        }
                        setsToRender = new LinkedList<>();
                        for (int i = 0; i < amount; i++) {
                            setsToRender.add("" + (i + 1));
                        }
                    }
                }

                if (setsToRender.isEmpty()) {
                    RichText richText = new RichText(new HashMap<>());
                    richText.setType(ElementType.Richtext);
                    richText.setContent("Keine Angaben");
                    resultList.add(new PdfElement(richText.getType(), richText, newIndent, null));
                } else {
                    var setCounter = 0;

                    for (String id : setsToRender) {
                        var headline = new Headline(new HashMap<>());
                        headline.setContent(replicatingContainerLayout.getHeadlineTemplate().replaceAll("#", "" + (setCounter + 1))); // TODO
                        headline.setSmall(true);
                        headline.setSize(6);
                        headline.setType(ElementType.Headline);
                        resultList.add(new PdfElement(ElementType.Headline, headline, newIndent, null, "group-start", "group-item"));

                        var newIdPrefix = (idPrefix.map(s -> s + "_").orElse("")) + currentElement.getId() + "_" + id;

                        children
                                .stream()
                                .map(child -> generatePdfElements(rootElement, Optional.of(newIdPrefix), child, newIndent, customerInput, scriptEngine))
                                .map(childElements -> childElements.stream().map(e -> e.withClasses("group-item")).toList())
                                .forEach(resultList::addAll);

                        resultList.set(resultList.size() - 1, resultList.getLast().withClasses("group-end", "group-item"));

                        setCounter++;
                    }
                }
            } else {
                children
                        .stream()
                        .map(child -> generatePdfElements(rootElement, idPrefix, child, currentIndent, customerInput, scriptEngine))
                        .forEach(resultList::addAll);
            }
        }

        return resultList;
    }
}
