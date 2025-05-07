package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.elements.models.form.input.TableField;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PdfElementsGenerator {

    // TODO: Maybe remove customer input optional and decide based on the form state
    public static List<PdfElement> generatePdfElements(
            RootElement rootElement,
            Optional<Map<String, Object>> customerInput,
            FormState formState,
            @Nonnull
            Boolean skipTechnical
    ) {
        var rootPdfElement = generatePdfElement(
                null,
                rootElement,
                customerInput.orElse(null),
                formState,
                skipTechnical
        );

        if (rootPdfElement == null) {
            return List.of();
        }

        return rootPdfElement.children();

        /*
        return generatePdfElement(
                null,
                rootElement,
                customerInput.orElse(null),
                formState
        ).children();
        return generatePdfElements(
                rootElement,
                Optional.empty(),
                rootElement,
                0,
                customerInput,
                formState
        );

         */
    }

    /**
     *
     * @param idPrefix
     * @param currentElement
     * @param customerInput If no customer input is given, the pdf elements are generated for a blank form print
     * @param formState
     * @return
     */
    @Nullable
    private static PdfElement generatePdfElement(
            @Nullable
            String idPrefix,
            @Nullable
            BaseElement currentElement,
            @Nullable
            Map<String, Object> customerInput,
            @Nonnull
            FormState formState,
            @Nonnull
            Boolean skipTechnical
    ) {
        // Check if the current element is null
        if (currentElement == null) {
            return null;
        }

        var resolvedId = currentElement
                .getResolvedId(idPrefix);

        // Check if the element was overridden. Check this only if customer input is present
        if (customerInput != null) {
            currentElement = formState
                    .overrides()
                    .getOrDefault(resolvedId, currentElement);
        }

        // Check if the element is technical or disabled
        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            if (skipTechnical && Boolean.TRUE.equals(baseInputElement.getTechnical())) {
                return null;
            }
        }

        // Check if the element is visible
        if (customerInput != null) {
            var isVisible = formState.visibilities().getOrDefault(resolvedId, true);
            if (!isVisible) {
                return null;
            }
        }

        Object value = null;
        if (customerInput != null && currentElement instanceof BaseInputElement<?> inputElement) {
            Object rawValue = formState.values().get(resolvedId);
            value = inputElement.formatValue(rawValue);
        }

        if (currentElement instanceof RootElement rootElement) {
            var children = rootElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(null, child, customerInput, formState, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof StepElement stepElement) {
            var children = stepElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(null, child, customerInput, formState, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof GroupLayout groupLayout) {
            var children = groupLayout
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(idPrefix, child, customerInput, formState, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof ReplicatingContainerLayout replicatingContainerLayout) {
            if (customerInput == null) {
                value = new LinkedList<String>();
                var amountOfPlaceholderDatasets = replicatingContainerLayout.getMaximumSets() != null ? replicatingContainerLayout.getMaximumSets() : 4;
                if (replicatingContainerLayout.getMinimumRequiredSets() != null && replicatingContainerLayout.getMinimumRequiredSets() > amountOfPlaceholderDatasets) {
                    amountOfPlaceholderDatasets = replicatingContainerLayout.getMinimumRequiredSets();
                }
                for (int i = 0; i < amountOfPlaceholderDatasets; i++) {
                    ((LinkedList<String>) value).add("placeholder_" + (i + 1));
                }
            }

            if (value instanceof Collection<?> cValue) {
                var childGroups = new LinkedList<PdfElement>();
                var index = 0;
                for (Object val : cValue) {
                    if (val instanceof String childId) {
                        var newIdPrefix = (idPrefix != null ? idPrefix + "_" : "") + currentElement.getId() + "_" + childId;
                        var children = replicatingContainerLayout
                                .getChildren()
                                .stream()
                                .map(child -> generatePdfElement(newIdPrefix, child, customerInput, formState, skipTechnical))
                                .filter(Objects::nonNull)
                                .toList();

                        var gl = new GroupLayout(Map.of());
                        gl.setType(ElementType.Group);

                        childGroups.add(new PdfElement(gl, index, children));
                        index++;
                    }
                }
                return new PdfElement(currentElement, cValue, childGroups);
            } else {
                return new PdfElement(currentElement, null, List.of());
            }
        } else {
            if (currentElement instanceof TableField tableElement && customerInput == null) {
                var placeholderRows = tableElement.getMaximumRows() != null ? tableElement.getMaximumRows() : (tableElement.getMinimumRequiredRows() != null ? tableElement.getMinimumRequiredRows() : 4);
                if (placeholderRows <= 0) {
                    placeholderRows = 4;
                }

                var values = new LinkedList<Map<String, Object>>();
                for (int i = 0; i < placeholderRows; i++) {
                    var row = new LinkedHashMap<String, Object>();
                    for (var field : tableElement.getFields()) {
                        row.put(field.getLabel(), "");
                    }
                    values.add(row);
                }
                return new PdfElement(currentElement, values, null);
            } else {
                return new PdfElement(currentElement, value, null);
            }
        }
    }

    /*

    private static List<PdfElement> generatePdfElements(
            RootElement rootElement,
            Optional<String> idPrefix,
            BaseElement _currentElement,
            int currentIndent,
            Optional<Map<String, Object>> customerInput,
            FormState formState
    ) {
        if (_currentElement == null) {
            return List.of();
        }

        var currentElement = customerInput.isPresent() ? formState.overrides().getOrDefault(_currentElement.getResolvedId(idPrefix.orElse(null)), _currentElement) : _currentElement;

        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            if (Boolean.TRUE.equals(baseInputElement.getTechnical())) {
                return List.of();
            }

            if (customerInput.isEmpty() && Boolean.TRUE.equals(baseInputElement.getDisabled())) {
                return List.of();
            }
        }

        if (customerInput.isPresent() && !formState.visibilities().getOrDefault(currentElement.getResolvedId(idPrefix.orElse(null)), true)) {
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
            Object rawValue = formState.values().get(currentElement.getResolvedId(idPrefix.orElse(null)));
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
                                .map(child -> generatePdfElements(rootElement, Optional.of(newIdPrefix), child, newIndent, customerInput, formState))
                                .map(childElements -> childElements.stream().map(e -> e.withClasses("group-item")).toList())
                                .forEach(resultList::addAll);

                        resultList.set(resultList.size() - 1, resultList.getLast().withClasses("group-end", "group-item"));

                        setCounter++;
                    }
                }
            } else {
                children
                        .stream()
                        .map(child -> generatePdfElements(rootElement, idPrefix, child, currentIndent, customerInput, formState))
                        .forEach(resultList::addAll);
            }
        }

        return resultList;
    }

     */
}
