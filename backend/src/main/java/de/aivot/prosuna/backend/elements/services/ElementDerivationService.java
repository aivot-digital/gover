package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.EffectiveValueSource;
import de.aivot.prosuna.backend.elements.exceptions.DerivationException;
import de.aivot.prosuna.backend.elements.models.*;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.InputElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.SummaryLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementFlattenUtils;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.javascript.exceptions.JavascriptException;
import de.aivot.prosuna.backend.javascript.models.JavascriptResult;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.utils.ElementResolver;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Centralizes runtime derivation for form elements so the rest of the system can work with one consistent interpretation of a form definition.
 * <p>
 * Form rendering, validation, submission payload generation and process views all depend on the same questions being answered in the same order: whether an element is overridden,
 * whether it is visible, which value is effectively active and whether that value is valid. Keeping that pipeline in one service prevents callers from re-implementing partial
 * logic and accidentally observing different runtime states for the same form data.
 * <p>
 * The service also preserves contextual behavior for replicated containers. Child rows derive their own values and errors against row-local data, while still being able to
 * reference root-level state when expressions or dependent selects need it.
 */
@Service
public class ElementDerivationService {
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final ElementDataTransformService elementDataTransformService;
    private final CodeListElementOptionsService codeListElementOptionsService;

    @Nonnull
    public DerivedRuntimeElementData derive(@Nonnull BaseElement element, @Nonnull AuthoredElementValues authoredElementValues) {
        var edr = new ElementDerivationRequest(
                element,
                authoredElementValues
        );

        return derive(edr);
    }

    /**
     * Wires the collaborators that execute dynamic expressions and expose the derived runtime state in the payload shape expected by those expressions.
     *
     * @param javascriptEngineFactoryService supplies fresh JavaScript engines so derivations do not share mutable evaluator state across requests
     * @param noCodeEvaluationService        evaluates declarative expressions so the derivation pipeline can treat scripted and no-code rules uniformly
     * @param elementDataTransformService    builds the `$` payload view because dynamic rules should reason about destination-shaped data instead of internal element-id maps
     */
    @Autowired
    public ElementDerivationService(
            JavascriptEngineFactoryService javascriptEngineFactoryService,
            NoCodeEvaluationService noCodeEvaluationService,
            ElementDataTransformService elementDataTransformService,
            CodeListElementOptionsService codeListElementOptionsService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.elementDataTransformService = elementDataTransformService;
        this.codeListElementOptionsService = codeListElementOptionsService;
    }

    @Nonnull
    public DerivedRuntimeElementData derive(@Nonnull ElementDerivationRequest request) {
        return derive(request, new IdentityDataMap(), new ElementDerivationLogger());
    }

    /**
     * Produces the runtime view of a form tree for one authored data set.
     * <p>
     * A single entry point matters because callers need the effective values and the computed element states to come from the same derivation pass. Running visibility, value
     * derivation and validation separately would allow later stages to observe a different state than earlier ones, especially once overrides and replicated rows are involved.
     *
     * @param request bundles the form tree, authored values and derivation options so the pipeline operates on one coherent snapshot
     * @param logger  captures expression output and failures because derivation is dynamic and must remain diagnosable without changing the return type
     * @return the effective values and computed element states that describe the form at runtime
     */
    @Nonnull
    public DerivedRuntimeElementData derive(@Nonnull ElementDerivationRequest request,
                                            @Nonnull IdentityDataMap identities,
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
                request.processExecutionData(),
                request.authoredElementValues(),
                effectiveElementValues,
                computedElementStates,
                request.derivationOptions(),
                true,
                identities,
                logger
        );

        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }

    /**
     * Walks the element tree in the order required to keep later decisions dependent on earlier ones.
     * <p>
     * Overrides are derived first because they may change the element definition that visibility, value and validation rules should use. Visibility is derived before values so
     * hidden elements do not accumulate stale runtime data unnecessarily. Values are derived before errors because validation must run against the value the user or the derivation
     * logic will actually see.
     * <p>
     * The recursive shape also preserves row-local state for replicating containers. Each repeated item gets its own authored/effective maps and child state container so
     * row-specific rules do not bleed into siblings.
     */
    private void derive(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ElementDerivationOptions options,
            @Nonnull Boolean isParentVisible,
            @Nonnull IdentityDataMap identities,
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
                        processExecutionData,
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
            actualElement = codeListElementOptionsService.resolve(actualElement);
            if (actualElement != currentElement) {
                elementState.setOverride(actualElement);
            }

            var childOptions = options.copyForUseInChild(currentElement.getId());
            // Check if it's a SummaryLayout and prevent the error derivation for all children because they cannot be changed by the user and potential errors cannot be fixed.
            if (actualElement instanceof SummaryLayoutElement) {
                childOptions.getSkipErrorsForElementIds().add(ElementDerivationOptions.ALL_ELEMENTS);
            }

            var isVisible = isParentVisible && deriveVisibility(
                    javascriptEngine,
                    rootElement,
                    actualElement,
                    authoredElementValues,
                    effectiveElementValues,
                    computedElementStates,
                    processExecutionData,
                    options,
                    logger
            );
            elementState.setVisible(isVisible);

            if (isVisible && actualElement instanceof InputElement<?> inputElement) {
                var hasAuthoredValue = authoredElementValues
                        .containsKey(currentElement.getId());
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
                        processExecutionData,
                        options,
                        hasAuthoredValue,
                        authoredValue,
                        elementState,
                        identities,
                        logger
                );
                effectiveValue = inputElement.formatValue(effectiveValue);
                if (effectiveValue instanceof String s) {
                    effectiveValue = s.trim();
                }
                effectiveElementValues.put(currentElement.getId(), effectiveValue);

                var err = deriveError(
                        javascriptEngine,
                        rootElement,
                        inputElement,
                        authoredElementValues,
                        effectiveElementValues,
                        computedElementStates,
                        processExecutionData,
                        options,
                        effectiveValue,
                        elementState,
                        logger
                );

                if (err != null) {
                    elementState.setError(err.error);
                    elementState.setErrorDetails(err.errorDetails);
                }
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
                    for (var itemIndex = 0; itemIndex < effectiveChildDataSetList.size(); itemIndex++) {
                        var rawEffectiveChildDataSet = effectiveChildDataSetList.get(itemIndex);
                        if (rawEffectiveChildDataSet instanceof ReplicatingContainerLayoutElementValue effectiveChildDataSet) {
                            // Create a new container for the computed element states of the current child data set.
                            // Add this to the list of computed element states of the replicating list container.
                            var childItemElementStates = new ComputedElementStates();
                            elementState
                                    .getSubStates()
                                    .add(ComputedElementSubState.of(effectiveChildDataSet.getId(), childItemElementStates));

                            var childAuthoredElementValues = effectiveChildDataSet.getValues() == null
                                    ? new AuthoredElementValues()
                                    : effectiveChildDataSet.getValues();
                            // Row-local effective values must be rebuilt from visible descendants only.
                            var childEffectiveElementValues = new EffectiveElementValues();

                            for (var currentChildElement : replicatingContainer.getChildren()) {
                                derive(
                                        javascriptEngine,
                                        rootElement,
                                        currentChildElement,
                                        rootAuthoredElementValues,
                                        rootEffectiveElementValues,
                                        processExecutionData,
                                        childAuthoredElementValues,
                                        childEffectiveElementValues,
                                        childItemElementStates,
                                        childOptions,
                                        isVisible,
                                        identities,
                                        logger
                                );
                            }

                            effectiveChildDataSet.setValues(childEffectiveElementValues.toAuthoredElementValues());
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
                            processExecutionData,
                            authoredElementValues,
                            effectiveElementValues,
                            computedElementStates,
                            childOptions,
                            isVisible,
                            identities,
                            logger
                    );
                }
            }
        } catch (Exception e) {
            logger.error(currentElement, e);
            elementState.setError(e.getMessage());
        }
    }


    /**
     * Resolves a runtime override when the form definition allows the current element to mutate itself dynamically.
     * <p>
     * Overrides exist so authors can adapt labels, options or other element properties to the current runtime context without duplicating the surrounding form structure. The
     * method keeps that flexibility constrained by forbidding changes to id and type, because the rest of the derivation pipeline relies on stable identity and stable element
     * semantics.
     *
     * @return the derived replacement element, or {@code null} when the original element definition should stay in effect
     * @throws DerivationException when dynamic override logic produces an invalid or structurally incompatible element
     */
    @Nullable
    private BaseElement deriveOverride(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ElementDerivationOptions options,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        if (options.containsSkipOverrides(currentElement.getId())) {
            return null;
        }

        var override = currentElement.getOverride();

        if (override == null || override.getType() == null) {
            return null; // No override to derive if the element has no override
        }

        // Determine if override generation should be done with JavaScript code
        if (override.getJavascriptCode() != null && override.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerProcessExecutionData(
                                processExecutionData
                                        .patchWithElementData(
                                                elementDataTransformService,
                                                rootElement,
                                                effectiveElementValues,
                                                computedElementStates
                                        )
                        )
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
                    throw new DerivationException(
                            currentElement,
                            "Der Datensatz für die dynamische Struktur des Elementes entspricht keinem bekannten Element."
                    );
                }

                // Overriding ids is not allowed, so we check if the ids match
                if (!Objects.equals(currentElement.getId(), resolvedElement.getId())) {
                    throw new DerivationException(
                            currentElement,
                            "Die ID (id) wird in der dynamischen Struktur des Elementes geändert. Dieses Verhalten wird nicht unterstützt."
                    );
                }

                // Overriding types is not allowed, so we check if the types match
                if (!Objects.equals(currentElement.getType(), resolvedElement.getType())) {
                    throw new DerivationException(
                            currentElement,
                            "Der Typ (type) wird in der dynamischen Struktur des Elementes geändert. Dieses Verhalten wird nicht unterstützt."
                    );
                }

                // Overriding destination keys is not allowed for input elements, so we check if the destination keys match when both the current and the resolved element are input elements
                if (currentElement instanceof BaseInputElement<?> i && resolvedElement instanceof BaseInputElement<?> r) {
                    if (!Objects.equals(i.getDestinationKey(), r.getDestinationKey())) {
                        throw new DerivationException(
                                currentElement,
                                "Der Datenschlüssel (destinationKey) wird in der dynamischen Struktur des Elementes geändert. Dieses Verhalten wird nicht unterstützt."
                        );
                    }
                }

                // Return the resolved element as the override
                return resolvedElement;
            } else {
                return null;
            }
        }

        // Determine if override generation should be done with a no code expression
        if (override.getFieldNoCodeMap() != null) {
            var elementMapToUpdate = JsonMapperFactory.getInstance()
                    .convertValue(currentElement, new TypeReference<Map<String, Object>>() {
                    });
            var patchedProcessExecutionData = processExecutionData.patchWithElementData(
                    elementDataTransformService,
                    rootElement,
                    effectiveElementValues,
                    computedElementStates
            );

            for (var entry : override.getFieldNoCodeMap().entrySet()) {
                var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

                var fieldName = entry.getKey();
                var noCodeExpression = entry.getValue();

                NoCodeResult res = noCodeEvaluationService.evaluate(
                        noCodeExpression,
                        accumulator,
                        patchedProcessExecutionData
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

    /**
     * Determines visibility after overrides have been applied so downstream logic reasons about the element definition that is actually active.
     * <p>
     * Visibility is evaluated early because hiding an element is meant to short-circuit further runtime work for that branch. This keeps invisible subtrees from producing
     * misleading values or validation errors while still allowing skip options to force a stable visible state when a caller intentionally wants to bypass the dynamic rule.
     *
     * @throws DerivationException when a configured visibility rule cannot be evaluated reliably
     */
    private boolean deriveVisibility(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ElementDerivationOptions options,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        if (options.containsSkipVisibilities(currentElement.getId())) {
            return true;
        }

        var vis = currentElement.getVisibility();

        if (vis == null || vis.getType() == null) {
            return true;
        }

        // Determine if visibility calculation should be done with JavaScript code
        if (vis.getJavascriptCode() != null && vis.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerProcessExecutionData(
                                processExecutionData
                                        .patchWithElementData(
                                                elementDataTransformService,
                                                rootElement,
                                                effectiveElementValues,
                                                computedElementStates
                                        )
                        )
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
            var patchedProcessExecutionData = processExecutionData.patchWithElementData(
                    elementDataTransformService,
                    rootElement,
                    effectiveElementValues,
                    computedElementStates
            );

            return noCodeEvaluationService
                    .evaluate(vis.getNoCode(), accumulator, patchedProcessExecutionData)
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

    /**
     * Chooses the effective value that should drive rendering, submission and validation for one input element.
     * <p>
     * User-authored data wins whenever it is present on an enabled field because preserving explicit user intent is more important than re-computing a default. Derived values are
     * only used when the element is configured to supply one and authored input should not take precedence. The method also records the value source so downstream consumers can
     * distinguish a retained user answer from a system-generated one.
     * <p>
     * Select values are sanitized before being accepted because dependent option lists can change as other inputs change. Keeping a now-invalid selection would make the runtime
     * state internally inconsistent even if the authored data was valid earlier.
     *
     * @return the value that should be treated as authoritative for the current runtime state
     * @throws DerivationException when dynamic value logic is configured but cannot yield a usable result
     */
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
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ElementDerivationOptions options,
            boolean hasAuthoredValue,
            @Nullable Object authoredValue,
            @Nonnull ComputedElementState elementState,
            @Nonnull IdentityDataMap identities,
            @Nonnull ElementDerivationLogger logger
    ) throws DerivationException {
        var baseElement = (BaseElement) inputElement;

        // Check if an identity mapping exists and assign the mapped value if possible and disable this field to prevent further changes.
        if (
                baseElement.getMetadata() != null &&
                        baseElement.getMetadata().getIdentitySourceId() != null &&
                        baseElement.getMetadata().getIdentityMappings() != null &&
                        !baseElement.getMetadata().getIdentityMappings().isEmpty() &&
                        identities.containsKey(baseElement.getMetadata().getIdentitySourceId())
        ) {
            IdentityData identityData = identities
                    .get(baseElement.getMetadata().getIdentitySourceId());

            if (identityData != null && identityData.type() == IdentityType.IdentityProvider) {
                String identityAttributeKey = baseElement
                        .getMetadata()
                        .getIdentityMappings()
                        .get(identityData.metadataIdentifier());

                if (StringUtils.isNotNullOrEmpty(identityAttributeKey)) {
                    Object attributeValue = identityData
                            .attributes()
                            .get(identityAttributeKey);
                    if (attributeValue != null) {
                        var formattedAttributeValue = inputElement.formatValue(attributeValue);

                        if (formattedAttributeValue != null) {
                            effectiveElementValues.put(inputElement.getId(), formattedAttributeValue);
                            elementState.setValueSource(EffectiveValueSource.Identity);
                            elementState.setDisabled(true);
                            return formattedAttributeValue;
                        }
                    }
                }
            }
        }

        if (options.containsSkipValues(inputElement.getId())) {
            effectiveElementValues.put(inputElement.getId(), authoredValue);
            elementState.setValueSource(EffectiveValueSource.Authored);
            return authoredValue;
        }

        var valueFunction = inputElement.getValue();

        // Key presence represents explicit user intent. A present null value is an authored clear,
        // while an absent key allows the dynamic value function to supply the effective value.
        if (valueFunction == null || valueFunction.getType() == null || (hasAuthoredValue && !Boolean.TRUE.equals(inputElement.getDisabled()))) {
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
                        .registerProcessExecutionData(
                                processExecutionData
                                        .patchWithElementData(
                                                elementDataTransformService,
                                                rootElement,
                                                effectiveElementValues,
                                                computedElementStates
                                        )
                        )
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
                var patchedProcessExecutionData = processExecutionData.patchWithElementData(
                        elementDataTransformService,
                        rootElement,
                        effectiveElementValues,
                        computedElementStates
                );

                var derivedValue = noCodeEvaluationService
                        .evaluate(valueFunction.getNoCode(), accumulator, patchedProcessExecutionData)
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

    /**
     * Removes stale values from dependent select elements before they enter the effective runtime state.
     * <p>
     * Dependent selects are valid only relative to the currently selected option of another select. A previously chosen option may stop belonging to the active group after the
     * referenced select changes. Returning {@code null} in that case forces the runtime state to reflect the currently available option set instead of silently preserving an
     * impossible selection.
     */
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

    /**
     * Resolves the select element that governs the current dependent select.
     * <p>
     * The lookup starts at the form root rather than the current branch because dependency references are id-based and should remain stable even when authors rearrange the layout
     * hierarchy around the fields.
     */
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

    /**
     * Reads the controlling select value from the nearest meaningful scope.
     * <p>
     * Replicated rows need local dependencies to win over root-level data, otherwise one row could accidentally validate itself against another row's selection. The fallback order
     * therefore prefers row-local effective data, then row-local authored data, and only then falls back to root-level state. A present null value stops that fallback because it
     * represents an explicit clear in the nearer scope.
     */
    @Nullable
    private String resolveReferencedSelectValue(
            @Nonnull SelectInputElement referencedSelectField,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull AuthoredElementValues rootAuthoredElementValues,
            @Nonnull EffectiveElementValues rootEffectiveElementValues
    ) {
        var referencedElementId = referencedSelectField.getId();
        Object rawValue;
        if (effectiveElementValues.containsKey(referencedElementId)) {
            rawValue = effectiveElementValues.get(referencedElementId);
        } else if (authoredElementValues.containsKey(referencedElementId)) {
            rawValue = authoredElementValues.get(referencedElementId);
        } else if (rootEffectiveElementValues.containsKey(referencedElementId)) {
            rawValue = rootEffectiveElementValues.get(referencedElementId);
        } else {
            rawValue = rootAuthoredElementValues.get(referencedElementId);
        }

        return referencedSelectField.formatValue(rawValue);
    }

    /**
     * Derives the validation error that should be exposed for the current input element.
     * <p>
     * Built-in validation runs before custom rules so structural guarantees such as requiredness and type-specific constraints are enforced consistently even when authors also
     * configured dynamic validation. Custom validation then refines that baseline with runtime-specific business rules. Returning the first relevant error keeps the element state
     * focused on the reason that matters most for the current input.
     * <p>
     * Skip options and nullable non-required values are handled early because callers sometimes need a pure derivation pass without validation noise, and optional empty fields
     * should not enter the expensive dynamic validation path.
     */
    @Nullable
    private ErrorResult deriveError(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull BaseElement rootElement,
            @Nonnull InputElement<?> inputElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull EffectiveElementValues effectiveElementValues,
            @Nonnull ComputedElementStates computedElementStates,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ElementDerivationOptions options,
            @Nullable Object effectiveValue,
            @Nonnull ComputedElementState elementState,
            @Nonnull ElementDerivationLogger logger
    ) {
        if (options.containsSkipErrors(inputElement.getId())) {
            return null;
        }

        if (inputElement instanceof BaseInputElement<?> baseInputElement &&
            Boolean.TRUE.equals(baseInputElement.getTechnical())) {
            return null; // No error if the input is a technical field because it is not visible to users.
        }

        var baseElement = (BaseElement) inputElement;

        if (effectiveValue == null && !Boolean.TRUE.equals(inputElement.getRequired())) {
            return null; // No error if the input is not required and no value is provided
        }

        try {
            inputElement.validate(effectiveValue);
        } catch (ValidationException e) {
            return ErrorResult.of(e);
        }

        var validation = inputElement
                .getValidation();

        if (validation == null || validation.getType() == null) {
            return null;
        }

        if (validation.getJavascriptCode() != null && validation.getJavascriptCode().isNotEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerProcessExecutionData(
                                processExecutionData
                                        .patchWithElementData(
                                                elementDataTransformService,
                                                rootElement,
                                                effectiveElementValues,
                                                computedElementStates
                                        )
                        )
                        .registerElementObject(baseElement)
                        .evaluateCode(validation.getJavascriptCode());
            } catch (JavascriptException e) {
                throw new RuntimeException(e);
            }

            logger.log(baseElement, res);

            var str = res.asString();
            if (StringUtils.isNotNullOrEmpty(str)) {
                return ErrorResult.of(str);
            }
            return null;
        }

        if (validation.getNoCodeList() != null && !validation.getNoCodeList().isEmpty()) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);
            var patchedProcessExecutionData = processExecutionData.patchWithElementData(
                    elementDataTransformService,
                    rootElement,
                    effectiveElementValues,
                    computedElementStates
            );

            for (var validationExpression : validation.getNoCodeList()) {
                var res = noCodeEvaluationService
                        .evaluate(validationExpression.getNoCode(), accumulator, patchedProcessExecutionData);
                if (!res.getValueAsBoolean()) {
                    return ErrorResult.of(Objects.requireNonNullElse(validationExpression.getMessage(), ""));
                }
            }
        }

        if (validation.getConditionSet() != null && rootElement instanceof LayoutElement<?> elementWithChildren) {
            var accumulator = createRuntimeAccumulator(computedElementStates, effectiveElementValues);

            var r = validation
                    .getConditionSet()
                    .evaluate(
                            elementWithChildren,
                            accumulator,
                            baseElement
                    );
            if (StringUtils.isNotNullOrEmpty(r)) {
                return ErrorResult.of(r);
            }
        }

        return null;
    }

    private record ErrorResult(
            @Nonnull
            String error,
            @Nullable
            Object errorDetails
    ) {
        public static ErrorResult of(String e) {
            return new ErrorResult(
                    e,
                    null
            );
        }

        public static ErrorResult of(ValidationException e) {
            return new ErrorResult(
                    e.getMessage(),
                    e.getErrorDetails()
            );
        }
    }

    /**
     * Creates the shared runtime snapshot that expression evaluators consume.
     * <p>
     * JavaScript and no-code rules should make decisions against the same in-flight derivation state that the service itself is building. Wrapping the current effective values and
     * computed states into one object ensures every evaluator sees the same snapshot and avoids ad-hoc argument lists that would drift apart over time.
     */
    private DerivedRuntimeElementData createRuntimeAccumulator(@Nonnull ComputedElementStates computedElementStates,
                                                               @Nonnull EffectiveElementValues effectiveElementValues) {
        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }

}
