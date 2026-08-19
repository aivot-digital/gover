package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.ComputedElementSubState;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.IntroductionStepElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class PdfElementsGenerator {
    private static final int BLANK_PRINT_PLACEHOLDER_COUNT = 5;

    // TODO: Maybe remove customer input optional and decide based on the form state
    public static List<PdfElement> generatePdfElements(
            @Nonnull FormLayoutElement rootElement,
            @Nullable DerivedRuntimeElementData elementData,
            @Nonnull Boolean skipTechnical
    ) {
        return generatePdfElements(rootElement, elementData, skipTechnical, elementData == null);
    }

    public static List<PdfElement> generatePdfElements(
            @Nonnull FormLayoutElement rootElement,
            @Nullable DerivedRuntimeElementData elementData,
            @Nonnull Boolean skipTechnical,
            boolean blankPrint
    ) {
        var rootPdfElement = generatePdfElement(
                rootElement,
                elementData,
                skipTechnical,
                blankPrint
        );

        if (rootPdfElement == null) {
            return List.of();
        }

        return rootPdfElement.children();
    }

    public static int getBlankPrintPlaceholderCount(@Nonnull ReplicatingContainerLayoutElement replicatingContainerLayout) {
        var amountOfPlaceholderDatasets = replicatingContainerLayout.getMaximumSets() != null && replicatingContainerLayout.getMaximumSets() > 0
                ? replicatingContainerLayout.getMaximumSets()
                : BLANK_PRINT_PLACEHOLDER_COUNT;
        if (replicatingContainerLayout.getMinimumRequiredSets() != null && replicatingContainerLayout.getMinimumRequiredSets() > amountOfPlaceholderDatasets) {
            amountOfPlaceholderDatasets = replicatingContainerLayout.getMinimumRequiredSets();
        }
        return amountOfPlaceholderDatasets;
    }

    @Nullable
    private static EffectiveElementValues resolveReplicatingContainerRowValues(@Nullable Object row) {
        if (!(row instanceof ReplicatingContainerLayoutElementValue) && !(row instanceof Map<?, ?>)) {
            return null;
        }

        var rows = ReplicatingContainerLayoutElement._formatValue(List.of(row));
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        var authoredValues = rows.getFirst().getValues();
        var effectiveValues = new EffectiveElementValues();
        if (authoredValues != null) {
            effectiveValues.putAll(authoredValues);
        }
        return effectiveValues;
    }

    @Nullable
    private static String resolveReplicatingContainerRowId(@Nullable Object row) {
        if (row == null) {
            return null;
        }

        var rows = ReplicatingContainerLayoutElement._formatValue(List.of(row));
        return rows == null || rows.isEmpty() ? null : rows.getFirst().getId();
    }

    @Nonnull
    private static ComputedElementStates resolveReplicatingContainerRowStates(@Nullable ComputedElementState elementState,
                                                                              @Nullable Object row,
                                                                              int index) {
        var subStates = elementState != null ? elementState.getSubStates() : null;
        if (subStates == null) {
            return new ComputedElementStates();
        }

        var rowId = resolveReplicatingContainerRowId(row);
        if (rowId != null) {
            for (ComputedElementSubState subState : subStates) {
                if (rowId.equals(subState.getId())) {
                    return subState.getStates();
                }
            }
        }

        return index >= 0 && index < subStates.size() ? subStates.get(index).getStates() : new ComputedElementStates();
    }

    @Nullable
    private static PdfElement generatePdfElement(
            @Nullable
            BaseElement currentElement,
            @Nullable
            DerivedRuntimeElementData customerInput,
            @Nonnull
            Boolean skipTechnical,
            boolean blankPrint
    ) {
        // Check if the current element is null
        if (currentElement == null) {
            return null;
        }

        var elementState = customerInput != null
                ? customerInput.getElementStates().getOrDefault(currentElement.getId(), ComputedElementState.create())
                : null;

        // Check if the element was overridden. Check this only if customer input is present
        if (elementState != null) {
            var override = elementState.getOverride();
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
        if (elementState != null && !blankPrint) {
            var isVisible = elementState.getVisible();
            if (!isVisible) {
                return null;
            }
        }

        Object value = null;
        if (customerInput != null && currentElement instanceof BaseInputElement<?> inputElement) {
            Object rawValue = customerInput.getEffectiveValues().getOrDefault(currentElement.getId(), null);
            value = inputElement.formatValue(rawValue);
        }

        if (currentElement instanceof FormLayoutElement rootElement) {
            var children = rootElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical, blankPrint))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof GenericStepElement stepElement) {
            var children = stepElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical, blankPrint))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        }  else if (currentElement instanceof IntroductionStepElement stepElement) {
            var children = stepElement
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical, blankPrint))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof GroupLayoutElement groupLayout) {
            var children = groupLayout
                    .getChildren()
                    .stream()
                    .map(child -> generatePdfElement(child, customerInput, skipTechnical, blankPrint))
                    .filter(Objects::nonNull)
                    .toList();
            return new PdfElement(currentElement, null, children);
        } else if (currentElement instanceof ReplicatingContainerLayoutElement replicatingContainerLayout) {
            if (blankPrint) {
                var placeholderValues = new LinkedList<DerivedRuntimeElementData>();
                var amountOfPlaceholderDatasets = getBlankPrintPlaceholderCount(replicatingContainerLayout);
                for (int i = 0; i < amountOfPlaceholderDatasets; i++) {
                    var childStates = resolveReplicatingContainerRowStates(elementState, null, i);
                    placeholderValues.add(new DerivedRuntimeElementData(new EffectiveElementValues(), childStates));
                }
                value = placeholderValues;
            }

            if (value instanceof Collection<?> cValue) {
                var childGroups = new LinkedList<PdfElement>();
                var index = 0;
                for (Object val : cValue) {
                    final DerivedRuntimeElementData childElementData;
                    if (val instanceof DerivedRuntimeElementData derivedRuntimeElementData) {
                        childElementData = derivedRuntimeElementData;
                    } else {
                        var childEffectiveValues = resolveReplicatingContainerRowValues(val);
                        if (childEffectiveValues == null) {
                            index++;
                            continue;
                        }
                        var childStates = resolveReplicatingContainerRowStates(elementState, val, index);
                        childElementData = new DerivedRuntimeElementData(childEffectiveValues, childStates);
                    }

                    var children = replicatingContainerLayout
                            .getChildren()
                            .stream()
                            .map(child -> generatePdfElement(child, childElementData, skipTechnical, blankPrint))
                            .filter(Objects::nonNull)
                            .toList();

                    var gl = new GroupLayoutElement();

                    childGroups.add(new PdfElement(gl, index, children));
                    index++;
                }
                return new PdfElement(currentElement, cValue, childGroups);
            } else {
                return new PdfElement(currentElement, null, List.of());
            }
        } else {
            if (currentElement instanceof TableInputElement tableElement && blankPrint) {
                var placeholderRows = tableElement.getMaximumRows() != null && tableElement.getMaximumRows() > 0
                        ? tableElement.getMaximumRows()
                        : BLANK_PRINT_PLACEHOLDER_COUNT;
                if (tableElement.getMinimumRequiredRows() != null && tableElement.getMinimumRequiredRows() > placeholderRows) {
                    placeholderRows = tableElement.getMinimumRequiredRows();
                }

                var values = new LinkedList<Map<String, Object>>();
                for (int i = 0; i < placeholderRows; i++) {
                    var row = new LinkedHashMap<String, Object>();
                    for (var field : tableElement.getFields()) {
                        row.put(field.getKey(), "");
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
