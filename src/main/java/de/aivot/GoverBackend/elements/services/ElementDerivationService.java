package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.util.LinkedList;

@Service
public class ElementDerivationService {
    private final ElementErrorDerivationService errorDerivationService;
    private final ElementOverrideDerivationService overrideDerivationService;
    private final ElementValueDerivationService valueDerivationService;
    private final ElementVisibilityDerivationService visibilityDerivationService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    @Autowired
    public ElementDerivationService(
            ElementOverrideDerivationService overrideDerivationService,
            ElementVisibilityDerivationService visibilityDerivationService,
            ElementErrorDerivationService errorDerivationService,
            ElementValueDerivationService valueDerivationService,
            JavascriptEngineFactoryService javascriptEngineFactoryService,
            NoCodeEvaluationService noCodeEvaluationService
    ) {
        this.overrideDerivationService = overrideDerivationService;
        this.visibilityDerivationService = visibilityDerivationService;
        this.errorDerivationService = errorDerivationService;
        this.valueDerivationService = valueDerivationService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    public ElementData derive(@Nonnull ElementDerivationRequest request,
                              @Nonnull ElementDerivationLogger logger) {
        var javascriptEngine = javascriptEngineFactoryService
                .getEngine();

        var outputElementData = new ElementData();

        derive(
                javascriptEngine,
                request.getElement(),
                request.getElementData(),
                outputElementData,
                request.getElement(),
                request.getOptions(),
                true,
                logger
        );

        try {
            javascriptEngine.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outputElementData;
    }

    private void derive(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData inputContextElementData,
            @Nonnull ElementData outputContextElementData,
            @Nonnull BaseElement currentlyDerivedElement,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        switch (currentlyDerivedElement) {
            case RootElement element:
                deriveRootElement(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        element,
                        options,
                        isParentVisible,
                        logger);
                break;

            case StepElement element:
                deriveStepElement(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        element,
                        options,
                        isParentVisible,
                        logger);
                break;

            case GroupLayout element:
                deriveGroupLayout(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        element,
                        options,
                        isParentVisible,
                        logger);
                break;

            case ReplicatingContainerLayout element:
                deriveReplicatingLayout(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        element,
                        options,
                        isParentVisible,
                        logger);
                break;

            case BaseElement element:
                var elementDataObject = deriveElement(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        element,
                        options,
                        isParentVisible,
                        logger);

                inputContextElementData.put(element, elementDataObject);
                outputContextElementData.put(element, elementDataObject);
                break;
        }
    }

    private void deriveRootElement(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData inputContextElementData,
            @Nonnull ElementData outputContextElementData,
            @Nonnull RootElement currentRootElement,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        var dataObject = deriveElement(javascriptEngine,
                rootElement,
                inputContextElementData,
                currentRootElement,
                options,
                isParentVisible,
                logger);
        inputContextElementData.put(rootElement, dataObject);
        outputContextElementData.put(rootElement, dataObject);

        var isVisible = isParentVisible && dataObject.getIsVisible();

        var optionsForChildren = options.copyForUseInChild(currentRootElement.getId());

        if (currentRootElement.getIntroductionStep() != null) {
            var cDo = deriveElement(javascriptEngine,
                    rootElement,
                    inputContextElementData,
                    currentRootElement.getIntroductionStep(),
                    optionsForChildren,
                    isVisible,
                    logger);
            inputContextElementData.put(currentRootElement.getIntroductionStep(), cDo);
            outputContextElementData.put(currentRootElement.getIntroductionStep(), cDo);
        }

        if (currentRootElement.getSummaryStep() != null) {
            var cDo = deriveElement(javascriptEngine,
                    rootElement,
                    inputContextElementData,
                    currentRootElement.getSummaryStep(),
                    optionsForChildren,
                    isVisible,
                    logger);
            inputContextElementData.put(currentRootElement.getSummaryStep(), cDo);
            outputContextElementData.put(currentRootElement.getSummaryStep(), cDo);
        }

        if (currentRootElement.getSubmitStep() != null) {
            var cDo = deriveElement(javascriptEngine,
                    rootElement,
                    inputContextElementData,
                    currentRootElement.getSubmitStep(),
                    optionsForChildren,
                    isVisible,
                    logger);
            inputContextElementData.put(currentRootElement.getSubmitStep(), cDo);
            outputContextElementData.put(currentRootElement.getSubmitStep(), cDo);
        }

        for (var step : currentRootElement.getChildren()) {
            derive(javascriptEngine,
                    rootElement,
                    inputContextElementData,
                    outputContextElementData,
                    step,
                    optionsForChildren,
                    isVisible,
                    logger);
        }
    }

    protected void deriveStepElement(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData inputContextElementData,
            @Nonnull ElementData outputContextElementData,
            @Nonnull StepElement _stepElement,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        var dataObject = deriveElement(javascriptEngine,
                rootElement,
                inputContextElementData,
                _stepElement,
                options,
                isParentVisible,
                logger);
        inputContextElementData.put(_stepElement, dataObject);
        outputContextElementData.put(_stepElement, dataObject);

        var isVisible = isParentVisible && dataObject.getIsVisible();

        var stepElement = (StepElement) dataObject
                .getComputedOverrideOrDefault(_stepElement);

        var optionsForChildren = options.copyForUseInChild(stepElement.getId());

        if (stepElement.getChildren() != null) {
            for (var child : stepElement.getChildren()) {
                derive(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        child,
                        optionsForChildren,
                        isVisible,
                        logger);
            }
        }
    }

    protected void deriveGroupLayout(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData inputContextElementData,
            @Nonnull ElementData outputContextElementData,
            @Nonnull GroupLayout _groupLayout,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        var dataObject = deriveElement(javascriptEngine,
                rootElement,
                inputContextElementData,
                _groupLayout,
                options,
                isParentVisible,
                logger);
        inputContextElementData.put(_groupLayout, dataObject);
        outputContextElementData.put(_groupLayout, dataObject);

        var isVisible = isParentVisible && dataObject.getIsVisible();

        var groupLayout = (GroupLayout) dataObject
                .getComputedOverrideOrDefault(_groupLayout);

        var optionsForChildren = options.copyForUseInChild(groupLayout.getId());

        if (groupLayout.getChildren() != null) {
            for (var child : groupLayout.getChildren()) {
                derive(javascriptEngine,
                        rootElement,
                        inputContextElementData,
                        outputContextElementData,
                        child,
                        optionsForChildren,
                        isVisible,
                        logger);
            }
        }
    }

    protected void deriveReplicatingLayout(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData inputContextElementData,
            @Nonnull ElementData outputContextElementData,
            @Nonnull ReplicatingContainerLayout _replicatingContainerLayout,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        var dataObject = deriveElement(javascriptEngine,
                rootElement,
                inputContextElementData,
                _replicatingContainerLayout,
                options,
                isParentVisible,
                logger);
        inputContextElementData.put(_replicatingContainerLayout, dataObject);
        outputContextElementData.put(_replicatingContainerLayout, dataObject);

        var isVisible = isParentVisible && dataObject.getIsVisible();

        var replicatingContainerLayout = (ReplicatingContainerLayout) dataObject
                .getComputedOverrideOrDefault(_replicatingContainerLayout);

        var optionsForChildren = options.copyForUseInChild(replicatingContainerLayout.getId());

        var value = dataObject
                .getValue();

        if (value == null) {
            dataObject.setInputValue(null);
            dataObject.setComputedValue(null);
        } else if (value instanceof Iterable<?> iValue) {
            if (replicatingContainerLayout.getChildren() != null) {
                var om = new ElementDataConverter();

                var resValue = new LinkedList<ElementData>();

                for (Object item : iValue) {
                    var childInputContextElementData = om
                            .convertObjectToEntityAttribute(item);

                    var childOutputContextElementData = new ElementData();

                    for (var child : replicatingContainerLayout.getChildren()) {
                        derive(javascriptEngine,
                                rootElement,
                                childInputContextElementData,
                                childOutputContextElementData,
                                child,
                                optionsForChildren,
                                isVisible,
                                logger);
                    }

                    resValue.add(childOutputContextElementData);
                }

                if (resValue.isEmpty()) {
                    dataObject.setInputValue(null);
                } else {
                    dataObject.setInputValue(resValue);
                }
            }
        } else {
            dataObject.addComputedError("Der Wert der replizierenden Liste muss eine Iterable in Form der Elementdaten sein. Es war jedoch " + (value.getClass().getSimpleName()) + ".");
        }
    }

    protected ElementDataObject deriveElement(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData accumulator,
            @Nonnull BaseElement _currentElement,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        // Get or create the data object for the current element
        var inputDataObject = accumulator
                .computeIfAbsent(_currentElement.getId(), id -> new ElementDataObject(_currentElement));
        var dataObject = new ElementDataObject(_currentElement)
                .setType(_currentElement.getType())
                .setInputValue(inputDataObject.getInputValue())
                .setPreviousInputValue(inputDataObject.getPreviousInputValue())
                .setIsDirty(inputDataObject.getIsDirty())
                .setIsPrefilled(inputDataObject.getIsPrefilled());

        // Set the dirty flag if the element has an input value
        if (dataObject.getInputValue() != null) {
            dataObject.setIsDirty(true);
        }

        // Check if the element cannot be input by the user and clean it up if necessary
        if (userCannotInputElement(_currentElement)) {
            dataObject.setInputValue(null);
            dataObject.setIsDirty(false);
        }

        BaseElement overriddenElement = null;
        if (options.containsSkipOverrides(_currentElement.getId())) {
            overriddenElement = inputDataObject.getComputedOverride();
        } else {
            try {
                overriddenElement = overrideDerivationService
                        .derive(rootElement,
                                accumulator,
                                dataObject,
                                _currentElement,
                                javascriptEngine,
                                noCodeEvaluationService,
                                logger);
            } catch (DerivationException e) {
                logger.error(e);
                dataObject.addComputedError(e.getMessage());
            }
        }
        dataObject.setComputedOverride(overriddenElement);

        // If an override was applied, use it as the current element for further processing
        var currentElement = overriddenElement != null ?
                overriddenElement : _currentElement;

        // Check if the element cannot be input by the user and clean it up if necessary
        if (userCannotInputElement(currentElement)) {
            dataObject.setInputValue(null);
            dataObject.setIsDirty(false);
        }

        // Format the input value if the element is an input element
        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            var formattedValue = baseInputElement
                    .formatValue(dataObject.getInputValue());
            dataObject.setInputValue(formattedValue);
        }

        boolean computedVisibility = true;
        if (options.containsSkipVisibilities(currentElement.getId())) {
            computedVisibility = inputDataObject.getIsVisible();
        } else {
            try {
                computedVisibility = visibilityDerivationService
                        .derive(rootElement,
                                accumulator,
                                currentElement,
                                isParentVisible,
                                javascriptEngine,
                                noCodeEvaluationService,
                                logger);
            } catch (DerivationException e) {
                logger.error(e);
                dataObject.addComputedError(e.getMessage());
            }
        }
        dataObject.setIsVisible(computedVisibility);

        var isVisible = isParentVisible && computedVisibility;

        if (currentElement instanceof BaseInputElement<?> baseInputElement) {
            if (isVisible) {
                if (dataObject.getInputValue() == null && dataObject.getPreviousInputValue() != null) {
                    dataObject.setInputValue(dataObject.getPreviousInputValue());
                    dataObject.setPreviousInputValue(null);
                    dataObject.setIsDirty(true);
                }

                if (options.containsSkipValues(baseInputElement.getId())) {
                    dataObject.setComputedValue(inputDataObject.getComputedValue());
                } else {
                    try {
                        var computedValue = valueDerivationService
                                .derive(rootElement,
                                        accumulator,
                                        dataObject,
                                        baseInputElement,
                                        javascriptEngine,
                                        noCodeEvaluationService,
                                        logger);

                        if (baseInputElement.getType() == ElementType.Checkbox && computedValue == null) {
                            dataObject.setComputedValue(null);
                        } else {
                            var formattedComputedValue = baseInputElement
                                    .formatValue(computedValue);
                            dataObject
                                    .setComputedValue(formattedComputedValue);
                        }
                    } catch (DerivationException e) {
                        logger.error(e);
                        dataObject.addComputedError(e.getMessage());
                    }
                }

                if (options.containsSkipErrors(baseInputElement.getId())) {
                    dataObject.setComputedErrors(inputDataObject.getComputedErrors());
                } else {
                    try {
                        var computedErrors = errorDerivationService
                                .derive(rootElement,
                                        accumulator,
                                        dataObject,
                                        baseInputElement,
                                        javascriptEngine,
                                        noCodeEvaluationService,
                                        logger);
                        if (computedErrors != null) {
                            dataObject
                                    .addComputedError(computedErrors);
                        }
                    } catch (DerivationException e) {
                        logger.error(e);
                        dataObject.addComputedError(e.getMessage());
                    }
                }
            } else {
                if (dataObject.getInputValue() != null) {
                    dataObject.setPreviousInputValue(dataObject.getInputValue());
                }
                dataObject.setInputValue(null);
                dataObject.setComputedValue(null);
                dataObject.setIsDirty(false);
            }
        }

        return dataObject;
    }

    private static boolean userCannotInputElement(@Nonnull BaseElement element) {
        return !(element instanceof BaseInputElement<?> inputElement) ||
               Boolean.TRUE.equals(inputElement.getDisabled()) ||
               Boolean.TRUE.equals(inputElement.getTechnical());
    }
}
