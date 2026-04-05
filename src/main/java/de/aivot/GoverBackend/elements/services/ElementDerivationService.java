package de.aivot.GoverBackend.elements.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.EffectiveValueSource;
import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.*;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.InputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementFlattenUtils;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.javascript.exceptions.JavascriptException;
import de.aivot.GoverBackend.javascript.models.JavascriptResult;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Derivation service for elements.
 */
@Service
public class ElementDerivationService {
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final ElementDataTransformService elementDataTransformService;

    @Autowired
    public ElementDerivationService(
            JavascriptEngineFactoryService javascriptEngineFactoryService,
            NoCodeEvaluationService noCodeEvaluationService,
            ElementDataTransformService elementDataTransformService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.elementDataTransformService = elementDataTransformService;
    }

    @Nonnull
    public DerivedRuntimeElementData derive(@Nonnull ElementDerivationRequest request,
                                            @Nonnull ElementDerivationLogger logger) {
        var javascriptEngine = javascriptEngineFactoryService
                .getEngine();

        // Create the values container for the effective values.
        // Effective values are either the computed values or authored values.
        var effectiveElementValues = new EffectiveElementValues();

        // Create the container for the computed element states.
        // These contain metainformation about elements.
        var computedElementStates = new ComputedElementStates();

        derive(
                javascriptEngine,
                request.element(),
                request.element(),
                request.authoredElementValues(),
                effectiveElementValues,
                request.authoredElementValues(),
                effectiveElementValues,
                computedElementStates,
                request.derivationOptions(),
                true,
                logger
        );

        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }

    private void derive(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull ElementDerivationLogger logger
    ) {
        var elementState = new ComputedElementState();
        computedElementStates.put(currentElement.getId(), elementState);

        try {
            BaseElement overrideElement;
            if (isParentVisible) {
                overrideElement = deriveOverride(
                        javascriptEngine,
                        rootElement,
                        currentElement,
                        authoredElementValues,
                        effectiveElementValues,
                        computedElementStates,
                        options,
                        logger
                );
                elementState.setOverride(overrideElement);
            } else {
                overrideElement = null;
            }

            var actualElement = overrideElement != null
                    ? overrideElement
                    : currentElement;
            var childOptions = options.copyForUseInChild(currentElement.getId());

            var isVisible = isParentVisible && deriveVisibility(
                    javascriptEngine,
                    rootElement,
                    actualElement,
                    authoredElementValues,
                    effectiveElementValues,
                    computedElementStates,
                    options,
                    logger
            );
            elementState.setVisible(isVisible);

            if (isVisible && actualElement instanceof InputElement<?> inputElement) {
                var authoredValue = authoredElementValues
                        .getOrDefault(currentElement.getId(), null);

                var effectiveValue = deriveEffectiveValue(
                        javascriptEngine,
                        rootElement,
                        inputElement,
                        rootAuthoredElementValues,
                        rootEffectiveElementValues,
                        authoredElementValues,
                        effectiveElementValues,
                        computedElementStates,
                        options,
                        authoredValue,
                        elementState,
                        logger
                );
                effectiveElementValues.put(currentElement.getId(), effectiveValue);

                var err = deriveError(
                        javascriptEngine,
                        rootElement,
                        inputElement,
                        authoredElementValues,
                        effectiveElementValues,
                        computedElementStates,
                        options,
                        effectiveValue,
                        elementState,
                        logger
                );
                elementState.setError(err);
            }

            if (actualElement instanceof ReplicatingContainerLayoutElement replicatingContainer) {
                // Extract the effective child data set list as a raw object to work with.
                var rawEffectiveChildDataSetList = effectiveElementValues
                        .get(replicatingContainer.getId());

                // Create a new container list to hold the computed element states for all child data sets.
                // Add this to the existing element state of the replicating list container.
                elementState.setSubStates(new LinkedList<>());

                // Test if the child data is a list of child data sets.
                if (rawEffectiveChildDataSetList instanceof List<?> effectiveChildDataSetList) {
                    // Iterate through the list of all child data sets.
                    for (var rawEffectiveChildDataSet : effectiveChildDataSetList) {
                        if (rawEffectiveChildDataSet instanceof Map<?, ?> effectiveChildDataSet) {
                            @SuppressWarnings("unchecked")
                            var mutableEffectiveChildDataSet = (Map<String, Object>) effectiveChildDataSet;

                            // Create a new container for the computed element states of the current child data set.
                            // Add this to the list of computed element states of the replicating list container.
                            var childItemElementStates = new ComputedElementStates();
                            elementState
                                    .getSubStates()
                                    .add(childItemElementStates);

                            var om = ObjectMapperFactory.getInstance();
                            var childAuthoredElementValues = om.convertValue(mutableEffectiveChildDataSet, AuthoredElementValues.class);
                            var childEffectiveElementValues = om.convertValue(mutableEffectiveChildDataSet, EffectiveElementValues.class);

                            for (var currentChildElement : replicatingContainer.getChildren()) {
                                derive(
                                        javascriptEngine,
                                        rootElement,
                                        currentChildElement,
                                        rootAuthoredElementValues,
                                        rootEffectiveElementValues,
                                        childAuthoredElementValues,
                                        childEffectiveElementValues,
                                        childItemElementStates,
                                        childOptions,
                                        isVisible,
                                        logger
                                );
                            }

                            mutableEffectiveChildDataSet.clear();
                            mutableEffectiveChildDataSet.putAll(childEffectiveElementValues);
                        }
                    }
                }
            } else if (actualElement instanceof LayoutElement<?> layoutElement) {
                var children = layoutElement.getChildren();
                for (var child : children) {
                    derive(
                            javascriptEngine,
                            rootElement,
                            child,
                            rootAuthoredElementValues,
                            rootEffectiveElementValues,
                            authoredElementValues,
                            effectiveElementValues,
                            computedElementStates,
                            childOptions,
                            isVisible,
                            logger
                    );
                }
            }
        } catch (Exception e) {
            logger.error(currentElement, e);
            elementState.setError(e.getMessage());
        }
    }


    @Nullable
    private BaseElement deriveOverride(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        if (options.containsSkipOverrides(currentElement.getId())) {
            return null;
        }

        var override = currentElement.getOverride();

        if (override == null) {
            return null; // No override to derive if the element has no override
        }

        // Determine if override generation should be done with JavaScript code
        if (override.getJavascriptCode() != null && override.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerGlobalObject("$", elementDataTransformService.buildPayload(rootElement, effectiveElementValues))
                        .registerElementObject(currentElement)
                        .evaluateCode(override.getJavascriptCode());
            } catch (JavascriptException e) {
                throw new DerivationException(currentElement, "Fehler bei der Ausführung des Javascript-Codes für die Override-Ableitung: " + e.getMessage(), e);
            }

            // Check if the result is null, which indicates no override was generated
            if (res.isNull()) {
                return null;
            }

            // Log result output
            logger.log(currentElement, res);

            // Check if the result is a map, which indicates a valid override
            var resObject = res.asMap();

            // If the result is not a map, no valid override was generated
            if (resObject != null) {
                // Resolve the element from the map and check if a valid element was generated
                var resolvedElement = ElementResolver
                        .resolve(resObject);

                if (resolvedElement == null) {
                    throw new DerivationException(currentElement, "Der erzeugte Datensatz entspricht keinem bekannten Elementtyp");
                }

                // Overriding ids is not allowed, so we check if the ids match
                if (!Objects.equals(currentElement.getId(), resolvedElement.getId())) {
                    throw new DerivationException(currentElement, "Das abgeleitete Element hat eine andere ID als das ursprüngliche Element");
                }

                // Overriding types is not allowed, so we check if the types match
                if (!Objects.equals(currentElement.getType(), resolvedElement.getType())) {
                    throw new DerivationException(currentElement, "Das abgeleitete Element hat einen anderen Typ als das ursprüngliche Element");
                }

                // Return the resolved element as the override
                return resolvedElement;
            } else {
                return null;
            }
        }

        // Determine if override generation should be done with a no code expression
        if (override.getFieldNoCodeMap() != null) {
            var elementMapToUpdate = new ObjectMapper()
                    .convertValue(currentElement, new TypeReference<Map<String, Object>>() {
                    });

            for (var entry : override.getFieldNoCodeMap().entrySet()) {
                var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

                var fieldName = entry.getKey();
                var noCodeExpression = entry.getValue();

                NoCodeResult res = noCodeEvaluationService.evaluate(
                        noCodeExpression,
                        accumulator
                );

                elementMapToUpdate.put(fieldName, res);
            }

            var resolvedElement = ElementResolver
                    .resolve(elementMapToUpdate);

            if (resolvedElement == null) {
                throw new DerivationException(currentElement, "Der erzeugte Datensatz entspricht keinem bekannten Elementtyp.");
            }

            // Overriding ids is not allowed, so we check if the ids match
            if (!Objects.equals(currentElement.getId(), resolvedElement.getId())) {
                throw new DerivationException(currentElement, "Das abgeleitete Element hat eine andere ID als das ursprüngliche Element");
            }

            // Overriding types is not allowed, so we check if the types match
            if (!Objects.equals(currentElement.getType(), resolvedElement.getType())) {
                throw new DerivationException(currentElement, "Das abgeleitete Element hat einen anderen Typ als das ursprüngliche Element");
            }

            // Return the resolved element as the override
            return resolvedElement;
        }

        return null;
    }


    private boolean deriveVisibility(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        if (options.containsSkipVisibilities(currentElement.getId())) {
            return true;
        }

        var vis = currentElement.getVisibility();

        if (vis == null) {
            return true;
        }

        // Determine if visibility calculation should be done with JavaScript code
        if (vis.getJavascriptCode() != null && vis.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerGlobalObject("$", elementDataTransformService.buildPayload(rootElement, effectiveElementValues))
                        .registerElementObject(currentElement)
                        .evaluateCode(vis.getJavascriptCode());
            } catch (JavascriptException e) {
                throw new DerivationException(currentElement, "Fehler bei der Ausführung des Javascript-Codes für die Sichtbarkeits-Ableitung: " + e.getMessage(), e);
            }

            logger.log(currentElement, res);

            return Boolean.TRUE.equals(res.asBoolean());
        }

        // Determine if visibility calculation should be done with a no code expression
        if (vis.getNoCode() != null) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            return noCodeEvaluationService
                    .evaluate(vis.getNoCode(), accumulator)
                    .getValueAsBoolean();
        }

        // Determine if visibility calculation should be done with a function
        if (vis.getConditionSet() != null) {
            if (rootElement instanceof LayoutElement<?> elementWithChildren) {
                var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

                var res = vis
                        .getConditionSet()
                        .evaluate(
                                elementWithChildren,
                                accumulator,
                                currentElement
                        );

                return res == null;
            } else {
                throw new DerivationException(currentElement, "Die Sichtbarkeits-Ableitung mit einer Bedingungsfunktion ist nur für Elemente innerhalb von Layout-Elementen möglich.");
            }
        }

        return true;
    }

    @Nullable
    private Object deriveEffectiveValue(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull InputElement<?> inputElement,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nullable Object authoredValue,
            @Nonnull ComputedElementState elementState,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        var baseElement = (BaseElement) inputElement;

        if (options.containsSkipValues(inputElement.getId())) {
            effectiveElementValues.put(inputElement.getId(), authoredValue);
            elementState.setValueSource(EffectiveValueSource.Authored);
            return authoredValue;
        }

        var valueFunction = inputElement.getValue();

        // If the value function is null, or an authored value exists and the element is not disabled, set the authored value as the effective value
        if (valueFunction == null || (authoredValue != null && !Boolean.TRUE.equals(inputElement.getDisabled()))) {
            var sanitizedValue = sanitizeSelectEffectiveValue(
                    rootElement,
                    inputElement,
                    authoredValue,
                    rootAuthoredElementValues,
                    rootEffectiveElementValues,
                    authoredElementValues,
                    effectiveElementValues
            );
            effectiveElementValues.put(inputElement.getId(), sanitizedValue);
            elementState.setValueSource(EffectiveValueSource.Authored);
            return sanitizedValue; // No value to derive if the element has no value setter
        }

        try {
            // Determine if the value computation should be done with JavaScript code
            if (valueFunction.getJavascriptCode() != null && valueFunction.getJavascriptCode().isNotEmpty()) {
                var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

                var res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerGlobalObject("$", elementDataTransformService.buildPayload(rootElement, effectiveElementValues))
                        .registerElementObject(baseElement)
                        .evaluateCode(valueFunction.getJavascriptCode());

                logger.log(baseElement, res);

                var sanitizedValue = sanitizeSelectEffectiveValue(
                        rootElement,
                        inputElement,
                        res.asObject(),
                        rootAuthoredElementValues,
                        rootEffectiveElementValues,
                        authoredElementValues,
                        effectiveElementValues
                );
                effectiveElementValues.put(inputElement.getId(), sanitizedValue);
                elementState.setValueSource(EffectiveValueSource.Derived);
                return sanitizedValue; // No value to derive if the element has no value setter
            }

            // Determine if the value computation should be done with a value expression
            if (valueFunction.getNoCode() != null) {
                var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

                var derivedValue = noCodeEvaluationService
                        .evaluate(valueFunction.getNoCode(), accumulator)
                        .getValue();

                var sanitizedValue = sanitizeSelectEffectiveValue(
                        rootElement,
                        inputElement,
                        derivedValue,
                        rootAuthoredElementValues,
                        rootEffectiveElementValues,
                        authoredElementValues,
                        effectiveElementValues
                );
                effectiveElementValues.put(inputElement.getId(), sanitizedValue);
                elementState.setValueSource(EffectiveValueSource.Derived);
                return sanitizedValue;
            }
        } catch (Exception e) {
            throw new DerivationException(baseElement, "Bei der Erzeugung des dynamischen Wertes ist ein Fehler aufgetreten: " + e.getMessage(), e);
        }

        throw new DerivationException(baseElement, "Der Wert konnte nicht abgeleitet werden, da die Definition des Werteableitungsmechanismus ungültig ist.");
    }

    @Nullable
    private Object sanitizeSelectEffectiveValue(
            @Nonnull BaseElement rootElement,
            @Nonnull InputElement<?> inputElement,
            @Nullable Object effectiveValue,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues
    ) {
        if (!(inputElement instanceof SelectInputElement selectField)) {
            return effectiveValue;
        }

        if (StringUtils.isNullOrEmpty(selectField.getDependsOnSelectFieldId())) {
            return effectiveValue;
        }

        var selectedValue = selectField.formatValue(effectiveValue);
        if (selectedValue == null || !selectField.containsOptionValue(selectedValue)) {
            return effectiveValue;
        }

        var referencedSelectField = resolveReferencedSelectField(rootElement, selectField.getDependsOnSelectFieldId());
        if (referencedSelectField == null) {
            return effectiveValue;
        }

        var referencedValue = resolveReferencedSelectValue(
                referencedSelectField,
                authoredElementValues,
                effectiveElementValues,
                rootAuthoredElementValues,
                rootEffectiveElementValues
        );

        if (selectField.containsOptionValueForGroup(selectedValue, referencedValue)) {
            return selectedValue;
        }

        return null;
    }

    @Nullable
    private SelectInputElement resolveReferencedSelectField(
            @Nonnull BaseElement rootElement,
            @Nullable String referencedElementId
    ) {
        if (StringUtils.isNullOrEmpty(referencedElementId)) {
            return null;
        }

        return ElementFlattenUtils
                .flattenElements(rootElement)
                .stream()
                .filter(SelectInputElement.class::isInstance)
                .map(SelectInputElement.class::cast)
                .filter(element -> Objects.equals(element.getId(), referencedElementId))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private String resolveReferencedSelectValue(
            @Nonnull SelectInputElement referencedSelectField,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues
    ) {
        var rawValue = effectiveElementValues.get(referencedSelectField.getId());
        if (rawValue == null) {
            rawValue = authoredElementValues.get(referencedSelectField.getId());
        }
        if (rawValue == null) {
            rawValue = rootEffectiveElementValues.get(referencedSelectField.getId());
        }
        if (rawValue == null) {
            rawValue = rootAuthoredElementValues.get(referencedSelectField.getId());
        }

        return referencedSelectField.formatValue(rawValue);
    }

    @Nullable
    private String deriveError(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull InputElement<?> inputElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nullable Object effectiveValue,
            @Nonnull ComputedElementState elementState,
            @Nonnull ElementDerivationLogger logger
    ) {
        if (options.containsSkipErrors(inputElement.getId())) {
            return null;
        }

        var baseElement = (BaseElement) inputElement;

        if (effectiveValue == null && !Boolean.TRUE.equals(inputElement.getRequired())) {
            return null; // No error if the input is not required and no value is provided
        }

        try {
            inputElement.validate(effectiveValue);
        } catch (ValidationException e) {
            return e.getMessage();
        }

        var validation = inputElement
                .getValidation();

        if (validation == null) {
            return null;
        }

        if (validation.getJavascriptCode() != null && validation.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerGlobalObject("$", elementDataTransformService.buildPayload(rootElement, effectiveElementValues))
                        .registerElementObject(baseElement)
                        .evaluateCode(validation.getJavascriptCode());
            } catch (JavascriptException e) {
                throw new RuntimeException(e);
            }

            logger.log(baseElement, res);

            var str = res.asString();
            if (StringUtils.isNotNullOrEmpty(str)) {
                return str;
            }
            return null;
        }

        if (validation.getNoCodeList() != null && !validation.getNoCodeList().isEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            for (var validationExpression : validation.getNoCodeList()) {
                var res = noCodeEvaluationService
                        .evaluate(validationExpression.getNoCode(), accumulator);
                if (!res.getValueAsBoolean()) {
                    return validationExpression.getMessage();
                }
            }
        }

        if (validation.getConditionSet() != null && rootElement instanceof LayoutElement<?> elementWithChildren) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            return validation
                    .getConditionSet()
                    .evaluate(
                            elementWithChildren,
                            accumulator,
                            baseElement
                    );
        }

        return null;
    }

    private DerivedRuntimeElementData createRuntimeAccumulator(@Nonnull ComputedElementStates computedElementStates,
                                                               @Nonnull EffectiveElementValues effectiveElementValues) {
        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }
}
