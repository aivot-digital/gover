package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

public class ElementData extends HashMap<String, ElementDataObject> implements Serializable {
    public ElementData() {
        super();
    }

    public ElementData(ElementData elementData) {
        super(elementData);
    }

    public static ElementData of(Object... keyValuePairs) {
        var elementData = new ElementData();

        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be in pairs.");
        }

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            var key = keyValuePairs[i];
            var value = keyValuePairs[i + 1];

            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Keys must be of type String.");
            }

            var dataObject = new ElementDataObject();
            dataObject.setInputValue(value);
            elementData.put((String) key, dataObject);
        }

        return elementData;
    }

    public ElementDataObject put(String key, ElementDataObject dataObject) {
        super.put(key, dataObject);
        return dataObject;
    }

    public ElementDataObject put(BaseElement element, ElementDataObject dataObject) {
        put(element.getId(), dataObject);
        return dataObject;
    }

    public ElementDataObject putInputValue(BaseElement element, Object value) {
            var elementDataObject = new ElementDataObject(element);
            elementDataObject.setInputValue(value);
            return put(element.getId(), elementDataObject);
    }

    public ElementDataObject putInputValue(String id, ElementType type, Object value) {
        var elementDataObject = new ElementDataObject(type);
        elementDataObject.setInputValue(value);
        return put(id, elementDataObject);
    }

    public Optional<ElementDataObject> getOpt(String key) {
        return Optional.ofNullable(get(key));
    }

    @Nonnull
    public ElementDataObject mustGet(BaseElement forElement) {
        return getOrDefault(forElement.getId(), ElementDataObject.of(forElement));
    }

    @JsonIgnore
    public boolean hasAnyError() {
        return values()
                .stream()
                .anyMatch(d -> (
                                       d.getComputedErrors() != null &&
                                       !d.getComputedErrors().isEmpty()
                               ) || (
                                       d.getValue() instanceof ElementData ed &&
                                       ed.hasAnyError()
                               ));
    }

    public static ElementData fromValueMap(BaseElement element, Map<String, Object> valueMap) {
        // Create an object to collect the element data in.
        var elementData = new ElementData();

        // Put an empty element data object for the root element into the new element data.
        // This is necessary to ensure that all elements have an entry in the element data.
        // We will save the element data object for later use.
        var elementDataObject = new ElementDataObject(element);
        elementData.put(element.getId(), elementDataObject);

        // If the root element is an instance of BaseInputElement and not a ReplicatingContainerLayout,
        // the value should be inserted into the element data object.
        // Replicating container layouts are excluded here, because they are handled separately.
        if (
                element instanceof BaseInputElement<?> &&
                !(element instanceof ReplicatingContainerLayoutElement) &&
                valueMap.containsKey(element.getId())
        ) {
            elementDataObject.setInputValue(valueMap.get(element.getId()));
            elementDataObject.setIsDirty(true);
        }

        // Pattern match the element type and handle it accordingly.
        // This is necessary to handle complex elements like the root element or replicating container layouts.
        // Also, elements with children are handled here.
        switch (element) {
            // RootElements are special elements that can contain multiple, additional steps,
            // which are held in the root element itself.
            // These steps are the introduction step, summary step and submit step.
            case FormLayoutElement _rootElement -> {
                if (_rootElement.getIntroductionStep() != null) {
                    var introductionStepValue = valueMap.get(_rootElement.getIntroductionStep().getId());
                    elementData.putInputValue(
                            _rootElement.getIntroductionStep(),
                            introductionStepValue
                    );
                }

                if (_rootElement.getSummaryStep() != null) {
                    var summaryStepValue = valueMap.get(_rootElement.getSummaryStep().getId());
                    elementData.putInputValue(
                            _rootElement.getSummaryStep(),
                            summaryStepValue
                    );
                }

                if (_rootElement.getSubmitStep() != null) {
                    var submitStepValue = valueMap.get(_rootElement.getSubmitStep().getId());
                    elementData.putInputValue(
                            _rootElement.getSubmitStep(),
                            submitStepValue
                    );
                }

                // If the root element has children, iterate over them and call this function recursively.
                // This is necessary to ensure that all elements in the root element are handled.
                if (_rootElement.getChildren() != null) {
                    for (var step : _rootElement.getChildren()) {
                        elementData.putAll(ElementData.fromValueMap(step, valueMap));
                    }
                }
            }
            // ReplicatingContainerLayouts are special elements that can contain multiple child elements.
            // These child elements are replicated for each value in this container.
            // A value in this container is a scope for all child elements.
            case ReplicatingContainerLayoutElement replicatingContainerLayout -> {
                // Extract the context in the source map for the replicating container layout.
                var rawReplicatingContainerContextsInSourceMap = valueMap
                        .get(replicatingContainerLayout.getId());

                // Check if the replicating container layout has children and if the values in the map are a collection.
                if (
                        replicatingContainerLayout.getChildren() != null &&
                        rawReplicatingContainerContextsInSourceMap instanceof Collection<?> replicatingContainerContextsInSourceMap
                ) {
                    // Create a new list of element data objects.
                    // This list will hold the element data containers for each "iteration" of the replicating container.
                    // Each iteration holds all values for all children of the replicating container.
                    var replicatingContainerElementDataList = new LinkedList<ElementData>();

                    // Iterate over the contexts in the source map for the replicating container layout.
                    // Each context represents a scope for the child elements of the replicating container.
                    for (var rawContextInSourceMap : replicatingContainerContextsInSourceMap) {
                        // If the context is a Map, we can create an ElementData for it.
                        // This element data will hold the values for the child elements of the replicating container.
                        if (rawContextInSourceMap instanceof Map<?, ?> contextInSourceMap) {
                            var childElementData = new ElementData();
                            var sourceContext = (Map<String, Object>) contextInSourceMap;

                            for (var replicatingContainerChildElement : replicatingContainerLayout.getChildren()) {
                                var _childElementData = ElementData
                                        .fromValueMap(replicatingContainerChildElement, sourceContext);

                                childElementData.putAll(_childElementData);
                            }

                            replicatingContainerElementDataList
                                    .add(childElementData);
                        }
                    }

                    elementDataObject.setInputValue(replicatingContainerElementDataList);
                }
            }
            // ElementWithChildren are elements that can contain multiple child elements.
            // These child elements are handled recursively.
            // If the element has children, iterate over them and call this function recursively.
            case LayoutElement<?> elementWithChildren -> {
                if (elementWithChildren.getChildren() != null) {
                    for (var child : elementWithChildren.getChildren()) {
                        var childElementData = ElementData.fromValueMap(child, valueMap);
                        elementData.putAll(childElementData);
                    }
                }
            }
            default -> {
                // Do Nothing
            }
        }

        return elementData;
    }

    /**
     * Transform a given element data container into a map of values.
     * Based on the given element, it will extract the values.
     * Elements with children will be handled recursively.
     *
     * @param element the element to extract the values for.
     * @param elementData the element data container to extract the values from
     * @return a map of values, where the keys are the element IDs and the values are the values of the elements.
     */
    public static Map<String, Object> toValueMap(BaseElement element, ElementData elementData) {
        var map = new HashMap<String, Object>();

        Consumer<BaseInputElement<?>> addValueToMap = (baseInputElement) -> {
            if (baseInputElement == null) {
                return;
            }
            var dataObject = elementData.get(baseInputElement.getId());
            if (dataObject != null) {
                map.put(baseInputElement.getId(), dataObject.getValue());
            } else {
                map.put(baseInputElement.getId(), null);
            }
        };

        // Add the input value for the element to the map, if the element is a BaseInputElement.
        if (element instanceof BaseInputElement<?> baseInputElement) {
            addValueToMap.accept(baseInputElement);
        }

        switch (element) {
            case FormLayoutElement _rootElement -> {
                addValueToMap.accept(_rootElement.getIntroductionStep());
                addValueToMap.accept(_rootElement.getSummaryStep());
                addValueToMap.accept(_rootElement.getSubmitStep());

                if (_rootElement.getChildren() != null) {
                    for (var step : _rootElement.getChildren()) {
                        map.putAll(toValueMap(step, elementData));
                    }
                }
            }
            case StepElement _stepElement -> {
                if (_stepElement.getChildren() != null) {
                    for (var child : _stepElement.getChildren()) {
                        map.putAll(toValueMap(child, elementData));
                    }
                }
            }
            case GroupLayoutElement _groupLayout -> {
                if (_groupLayout.getChildren() != null) {
                    for (var child : _groupLayout.getChildren()) {
                        map.putAll(toValueMap(child, elementData));
                    }
                }
            }
            case ReplicatingContainerLayoutElement repl -> {
                var childDataObject = elementData
                        .get(repl.getId());

                // Reset previously set value
                map.put(repl.getId(), null);

                if (childDataObject == null) {
                    break;
                }

                if (repl.getChildren() == null) {
                    break;
                }

                var dataSets = childDataObject
                        .getValue();

                if (!(dataSets instanceof Collection<?>)) {
                    break;
                }

                var valueMapDataSets = new LinkedList<Map<String, Object>>();

                for (var dataSet : (Collection<?>) dataSets) {
                    if (!(dataSet instanceof ElementData)) {
                        continue;
                    }

                    var dataSetED = (ElementData) dataSet;
                    var dataSetValueMap = new HashMap<String, Object>();
                    for (var childElement : repl.getChildren()) {
                        var childValueMap = toValueMap(childElement, dataSetED);
                        dataSetValueMap.putAll(childValueMap);
                    }
                    valueMapDataSets.add(dataSetValueMap);
                }

                map.put(repl.getId(), valueMapDataSets);
            }
            default -> {
                // Do Nothing
            }
        }

        return map;
    }
}
