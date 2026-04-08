package de.aivot.GoverBackend.elements.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.EffectiveValueSource;
import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.*;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
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
 * Centralizes runtime derivation for form elements so the rest of the system can work with one
 * consistent interpretation of a form definition.
 * <p>
 * Form rendering, validation, submission payload generation and process views all depend on the
 * same questions being answered in the same order: whether an element is overridden, whether it is
 * visible, which value is effectively active and whether that value is valid. Keeping that pipeline
 * in one service prevents callers from re-implementing partial logic and accidentally observing
 * different runtime states for the same form data.
 * <p>
 * The service also preserves contextual behavior for replicated containers. Child rows derive their
 * own values and errors against row-local data, while still being able to reference root-level
 * state when expressions or dependent selects need it.
 */
@Service
public class ElementDerivationService {
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final ElementDataTransformService elementDataTransformService;

    /**
     * Wires the collaborators that execute dynamic expressions and expose the derived runtime state
     * in the payload shape expected by those expressions.
     *
     * @param javascriptEngineFactoryService supplies fresh JavaScript engines so derivations do not
     *                                       share mutable evaluator state across requests
     * @param noCodeEvaluationService evaluates declarative expressions so the derivation pipeline can
     *                                treat scripted and no-code rules uniformly
     * @param elementDataTransformService builds the `$` payload view because dynamic rules should
     *                                    reason about destination-shaped data instead of internal
     *                                    element-id maps
     */
    @Autowired
    public ElementDerivationService(
            JavascriptEngineFactoryService javascriptEngineFactoryService,
            NoCodeEvaluationService noCodeEvaluationService,
            ElementDataTransformService elementDataTransformService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.elementDataTransformService = elementDataTransformService;
    }

    /**
     * Produces the runtime view of a form tree for one authored data set.
     * <p>
     * A single entry point matters because callers need the effective values and the computed
     * element states to come from the same derivation pass. Running visibility, value derivation and
     * validation separately would allow later stages to observe a different state than earlier ones,
     * especially once overrides and replicated rows are involved.
     *
     * @param request bundles the form tree, authored values and derivation options so the pipeline
     *                operates on one coherent snapshot
     * @param logger captures expression output and failures because derivation is dynamic and must
     *               remain diagnosable without changing the return type
     * @return the effective values and computed element states that describe the form at runtime
     */
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
                List.of(),
                List.of(),
                logger
        );

        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }

    /**
     * Walks the element tree in the order required to keep later decisions dependent on earlier
     * ones.
     * <p>
     * Overrides are derived first because they may change the element definition that visibility,
     * value and validation rules should use. Visibility is derived before values so hidden elements
     * do not accumulate stale runtime data unnecessarily. Values are derived before errors because
     * validation must run against the value the user or the derivation logic will actually see.
     * <p>
     * The recursive shape also preserves row-local state for replicating containers. Each repeated
     * item gets its own authored/effective maps and child state container so row-specific rules do
     * not bleed into siblings. The same recursion also carries payload-path context so each computed
     * state can describe where its value would land in the outbound payload without building that
     * payload eagerly.
     */
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
            @Nonnull List<String> destinationPathPrefixSegments,
            @Nonnull List<Integer> replicationIndices,
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
            // The payload location is structural metadata, so it should reflect override decisions
            // but remain available even when later visibility or validation logic short-circuits.
            elementState.setDestinationPath(resolveDestinationPath(
                    actualElement,
                    destinationPathPrefixSegments,
                    replicationIndices
            ));

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
                    for (var itemIndex = 0; itemIndex < effectiveChildDataSetList.size(); itemIndex++) {
                        var rawEffectiveChildDataSet = effectiveChildDataSetList.get(itemIndex);
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
                            var childReplicationIndices = appendReplicationIndex(
                                    replicationIndices,
                                    itemIndex
                            );
                            // A container destination key introduces a row-local payload root, so
                            // descendants inherit that concrete row path instead of the parent one.
                            var childDestinationPathPrefixSegments = resolveChildDestinationPathPrefixSegments(
                                    replicatingContainer,
                                    destinationPathPrefixSegments,
                                    replicationIndices,
                                    itemIndex
                            );

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
                                        childDestinationPathPrefixSegments,
                                        childReplicationIndices,
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
                            destinationPathPrefixSegments,
                            replicationIndices,
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
     * Resolves a runtime override when the form definition allows the current element to mutate
     * itself dynamically.
     * <p>
     * Overrides exist so authors can adapt labels, options or other element properties to the
     * current runtime context without duplicating the surrounding form structure. The method keeps
     * that flexibility constrained by forbidding changes to id and type, because the rest of the
     * derivation pipeline relies on stable identity and stable element semantics.
     *
     * @return the derived replacement element, or {@code null} when the original element definition
     * should stay in effect
     * @throws DerivationException when dynamic override logic produces an invalid or structurally
     *                             incompatible element
     */
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

    /**
     * Determines visibility after overrides have been applied so downstream logic reasons about the
     * element definition that is actually active.
     * <p>
     * Visibility is evaluated early because hiding an element is meant to short-circuit further
     * runtime work for that branch. This keeps invisible subtrees from producing misleading values
     * or validation errors while still allowing skip options to force a stable visible state when a
     * caller intentionally wants to bypass the dynamic rule.
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

    /**
     * Chooses the effective value that should drive rendering, submission and validation for one
     * input element.
     * <p>
     * User-authored data wins whenever it is present on an enabled field because preserving explicit
     * user intent is more important than re-computing a default. Derived values are only used when
     * the element is configured to supply one and authored input should not take precedence. The
     * method also records the value source so downstream consumers can distinguish a retained user
     * answer from a system-generated one.
     * <p>
     * Select values are sanitized before being accepted because dependent option lists can change as
     * other inputs change. Keeping a now-invalid selection would make the runtime state internally
     * inconsistent even if the authored data was valid earlier.
     *
     * @return the value that should be treated as authoritative for the current runtime state
     * @throws DerivationException when dynamic value logic is configured but cannot yield a usable
     *                             result
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
        if (valueFunction == null || valueFunction.getType() == null || (authoredValue != null && !Boolean.TRUE.equals(inputElement.getDisabled()))) {
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

    /**
     * Removes stale values from dependent select elements before they enter the effective runtime
     * state.
     * <p>
     * Dependent selects are valid only relative to the currently selected option of another select.
     * A previously chosen option may stop belonging to the active group after the referenced select
     * changes. Returning {@code null} in that case forces the runtime state to reflect the currently
     * available option set instead of silently preserving an impossible selection.
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
     * The lookup starts at the form root rather than the current branch because dependency
     * references are id-based and should remain stable even when authors rearrange the layout
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
     * Replicated rows need local dependencies to win over root-level data, otherwise one row could
     * accidentally validate itself against another row's selection. The fallback order therefore
     * prefers row-local effective data, then row-local authored data, and only then falls back to
     * root-level state.
     */
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

    /**
     * Derives the validation error that should be exposed for the current input element.
     * <p>
     * Built-in validation runs before custom rules so structural guarantees such as requiredness and
     * type-specific constraints are enforced consistently even when authors also configured dynamic
     * validation. Custom validation then refines that baseline with runtime-specific business rules.
     * Returning the first relevant error keeps the element state focused on the reason that matters
     * most for the current input.
     * <p>
     * Skip options and nullable non-required values are handled early because callers sometimes need
     * a pure derivation pass without validation noise, and optional empty fields should not enter the
     * expensive dynamic validation path.
     */
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

        if (validation == null || validation.getType() == null) {
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

    /**
     * Creates the shared runtime snapshot that expression evaluators consume.
     * <p>
     * JavaScript and no-code rules should make decisions against the same in-flight derivation state
     * that the service itself is building. Wrapping the current effective values and computed states
     * into one object ensures every evaluator sees the same snapshot and avoids ad-hoc argument lists
     * that would drift apart over time.
     */
    private DerivedRuntimeElementData createRuntimeAccumulator(@Nonnull ComputedElementStates computedElementStates,
                                                               @Nonnull EffectiveElementValues effectiveElementValues) {
        return new DerivedRuntimeElementData(
                effectiveElementValues,
                computedElementStates
        );
    }

    /**
     * Resolves the destination path that should be attached to the current computed state.
     * <p>
     * The path must be based on the effective element definition rather than the authored one,
     * otherwise overrides that change the destination key would leave the runtime metadata out of
     * sync with the payload export logic.
     */
    @Nullable
    private String resolveDestinationPath(@Nonnull BaseElement element,
                                          @Nonnull List<String> destinationPathPrefixSegments,
                                          @Nonnull List<Integer> replicationIndices) {
        if (!(element instanceof BaseInputElement<?> inputElement)) {
            return null;
        }

        return elementDataTransformService.resolveDestinationPath(
                inputElement.getDestinationKey(),
                destinationPathPrefixSegments,
                replicationIndices
        );
    }

    /**
     * Resolves the destination-path prefix that descendant states should inherit from a replicating
     * container row.
     * <p>
     * Containers with their own destination key create a row-local payload object first and only
     * then attach that object to the parent payload. Child states therefore need the container path
     * plus the current row index as an inherited prefix. Containers without a destination key do not
     * create such an intermediate payload root, so their children must keep using the existing
     * inherited prefix.
     */
    @Nonnull
    private List<String> resolveChildDestinationPathPrefixSegments(
            @Nonnull ReplicatingContainerLayoutElement replicatingContainer,
            @Nonnull List<String> destinationPathPrefixSegments,
            @Nonnull List<Integer> replicationIndices,
            int itemIndex
    ) {
        var containerDestinationPathSegments = elementDataTransformService.resolveDestinationPathSegments(
                replicatingContainer.getDestinationKey(),
                destinationPathPrefixSegments,
                replicationIndices
        );
        if (containerDestinationPathSegments.isEmpty()) {
            return destinationPathPrefixSegments;
        }

        var childDestinationPathPrefixSegments = new LinkedList<>(containerDestinationPathSegments);
        // The row index becomes part of the inherited prefix only when the container owns the
        // payload array, because descendants are nested inside the row object written at that slot.
        childDestinationPathPrefixSegments.add(String.valueOf(itemIndex));
        return childDestinationPathPrefixSegments;
    }

    /**
     * Extends replication context for the next replicated row.
     * <p>
     * Wildcard substitution is positional, so nested replicated structures need an ordered list of
     * indices instead of a single mutable cursor shared across recursion branches.
     */
    @Nonnull
    private List<Integer> appendReplicationIndex(@Nonnull List<Integer> replicationIndices,
                                                 int itemIndex) {
        var childReplicationIndices = new LinkedList<>(replicationIndices);
        childReplicationIndices.add(itemIndex);
        return childReplicationIndices;
    }
}
