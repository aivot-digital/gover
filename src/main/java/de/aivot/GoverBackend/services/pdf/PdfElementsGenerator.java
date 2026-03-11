package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TableInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class PdfElementsGenerator {

    // TODO: Maybe remove customer input optional and decide based on the form state
    public static List<PdfElement> generatePdfElements(
            @Nonnull FormLayoutElement rootElement,
            @Nullable ElementData elementData,
            @Nonnull Boolean skipTechnical
    ) {
        var rootPdfElement = generatePdfElement(
                rootElement,
                elementData,
                skipTechnical
        );

        if (rootPdfElement == null) {
            return List.of();
        }

        return rootPdfElement.children();
    }

    @Nullable
    private static PdfElement generatePdfElement(
            @Nullable
            BaseElement currentElement,
            @Nullable
            ElementData customerInput,
            @Nonnull
            Boolean skipTechnical
    ) {
        // Check if the current element is null
        if (currentElement == null) {
            return null;
        }

        var dataObject = customerInput != null ? customerInput
                .getOrDefault(currentElement.getId(), new ElementDataObject(currentElement)) : null;

        // Check if the element was overridden. Check this only if customer input is present
        if (dataObject != null) {
            var override = dataObject
                    .getComputedOverride();
            if (override != null) {
                currentElement = override;
            }
        }

        // Check if the element is technical or disabled
        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            if (skipTechnical && Boolean.TRUE.equals(baseInputElement.getTechnical())) {
                return null;
            }
        }

        // Check if the element is visible
        if (dataObject != null) {
            var isVisible = dataObject.getIsVisible();
            if (!isVisible) {
                return null;
            }
        }

        Object value = null;
        if (dataObject != null && currentElement instanceof BaseInputElement<?> inputElement) {
            Object rawValue = dataObject.getValue();
            value = inputElement.formatValue(rawValue);
        }

        if (currentElement instanceof FormLayoutElement rootElement) {
            var children = rootElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof GenericStepElement stepElement) {
            var children = stepElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof GroupLayoutElement groupLayout) {
            var children = groupLayout
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof ReplicatingContainerLayoutElement replicatingContainerLayout) {
            if (customerInput == null) {
                value = new LinkedList<String>();
                var amountOfPlaceholderDatasets = replicatingContainerLayout.getMaximumSets() != null ? replicatingContainerLayout.getMaximumSets() : 4;
                if (replicatingContainerLayout.getMinimumRequiredSets() != null && replicatingContainerLayout.getMinimumRequiredSets() > amountOfPlaceholderDatasets) {
                    amountOfPlaceholderDatasets = replicatingContainerLayout.getMinimumRequiredSets();
                }
                for (int i = 0; i < amountOfPlaceholderDatasets; i++) {
                    ((LinkedList<ElementData>) value).add(new ElementData());
                }
            }

            if (value instanceof Collection<?> cValue) {
                var childGroups = new LinkedList<PdfElement>();
                var index = 0;
                for (Object val : cValue) {
                    if (val instanceof ElementData childElementData) {
                        var children = replicatingContainerLayout
                                .getChildren()
                                .stream()
                                .map(child -> generatePdfElement(child, childElementData, skipTechnical))
                                .filter(Objects::nonNull)
                                .toList();

                        var gl = new GroupLayoutElement();

                        childGroups.add(new PdfElement(gl, index, children));
                        index++;
                    }
                }
                return new PdfElement(currentElement, cValue, childGroups);
            } else {
                return new PdfElement(currentElement, null, List.of());
            }
        } else {
            if (currentElement instanceof TableInputElement tableElement && customerInput == null) {
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
}
