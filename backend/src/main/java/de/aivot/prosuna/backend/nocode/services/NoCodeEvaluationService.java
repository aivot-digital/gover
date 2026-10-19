package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service evaluates no code expressions.
 * It uses the operators provided by the registered {@link NoCodeOperatorsProvider} to evaluate the expressions.
 */
@Service
public class NoCodeEvaluationService {
    private static final Logger logger = LoggerFactory.getLogger(NoCodeEvaluationService.class);

    private final Map<String, NoCodeOperator> noCodeOperatorProviders;

    @Autowired
    public NoCodeEvaluationService(List<NoCodeOperatorsProvider> noCodeOperatorProviders) {
        this.noCodeOperatorProviders = new HashMap<>();

        for (var provider : noCodeOperatorProviders) {
            for (var operator : provider.getOperators()) {
                var packageScopedOperatorIdentifier = operator.getIdentifier();
                if (this.noCodeOperatorProviders.containsKey(packageScopedOperatorIdentifier)) {
                    logger
                            .atWarn()
                            .setMessage("No code operator with identifier " + packageScopedOperatorIdentifier + " already exists. Skipping this operator.")
                            .addKeyValue("operatorIdentifier", packageScopedOperatorIdentifier)
                            .addKeyValue("providerClassName", provider.getClass().getName())
                            .addKeyValue("operatorClassName", operator.getClass().getName())
                            .addKeyValue("operatorPackageName", operator.getClass().getPackageName())
                            .log();
                } else {
                    this.noCodeOperatorProviders.put(packageScopedOperatorIdentifier, operator);
                }
            }
        }
    }

    /**
     * Get the {@link NoCodeOperator} with the given identifier.
     *
     * @param identifier The identifier of the operator
     * @return The operator
     */
    private NoCodeOperator getNoCodeOperator(@Nullable String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("NoCodeOperatorProvider identifier must not be null");
        }

        if (!noCodeOperatorProviders.containsKey(identifier)) {
            throw new IllegalArgumentException("NoCodeOperatorProvider with identifier " + identifier + " does not exist");
        }
        return noCodeOperatorProviders.get(identifier);
    }

    /**
     * Evaluate a no code expression.
     *
     * @param operand     The expression to evaluate
     * @param elementData The form state, containing the values and visibilities of the elements
     * @return The result of the evaluation
     */
    @Nonnull
    public NoCodeResult evaluate(@Nullable NoCodeOperand operand, @Nonnull DerivedRuntimeElementData elementData) {
        return evaluate(operand, elementData, new ProcessExecutionData());
    }

    @Nonnull
    public NoCodeResult evaluate(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext
    ) {
        return evaluate(operand, elementData, processDataContext, null);
    }

    @Nonnull
    public NoCodeResult evaluate(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nullable List<Integer> wildcardIndices
    ) {
        if (operand == null) {
            return new NoCodeResult(null);
        }

        return switch (operand) {
            // Static values resolve directly
            case NoCodeStaticValue staticValue -> {
                var value = staticValue.getValue();
                yield new NoCodeResult(value);
            }

            // Referenced elements resolve based on their visibility and the values map
            case NoCodeReference reference -> {
                var elementId = reference.getElementId();
                var value = elementId == null
                        ? null
                        : elementData.getEffectiveValues().getOrDefault(elementId, null);
                yield new NoCodeResult(value);
            }

            // Process data references resolve based on the injected process context map
            case NoCodeProcessDataReference processDataReference -> {
                var value = ProcessDataValueUtils.resolveProcessDataValue(
                        processDataContext,
                        processDataReference.getPath(),
                        resolveWildcardIndices(processDataReference.getPath(), wildcardIndices, false)
                );
                yield new NoCodeResult(value);
            }

            case NoCodeInstanceDataReference instanceDataReference -> {
                var sourceData = processDataContext.get(ProcessExecutionData.PROCESS_METADATA_KEY);
                var value = ProcessDataValueUtils.resolveDestinationKeyValue(
                        sourceData,
                        instanceDataReference.getPath(),
                        resolveWildcardIndices(instanceDataReference.getPath(), wildcardIndices, false)
                );
                yield new NoCodeResult(value);
            }

            case NoCodeNodeDataReference nodeDataReference -> {
                var sourceData = resolveNodeData(nodeDataReference, processDataContext);
                var value = ProcessDataValueUtils.resolveDestinationKeyValue(
                        sourceData,
                        nodeDataReference.getPath(),
                        resolveWildcardIndices(nodeDataReference.getPath(), wildcardIndices, true)
                );
                yield new NoCodeResult(value);
            }

            // Expressions resolve by evaluating them
            case NoCodeExpression expression -> evaluateNoCodeExpression(expression, elementData, processDataContext, wildcardIndices);

            // Unknown operands are not supported
            default -> throw new IllegalStateException("Unexpected value: " + operand);
        };
    }

    @Nonnull
    private NoCodeResult evaluateNoCodeExpression(
            @Nonnull NoCodeExpression expression,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nullable List<Integer> wildcardIndices
    ) {
        var operands = expression.getOperands();
        if (operands == null) {
            operands = List.of();
        }

        var operandValues = operands
                .stream()
                .map(op -> evaluate(op, elementData, processDataContext, wildcardIndices))
                .map(NoCodeResult::getValue)
                .toArray();

        var operator = getNoCodeOperator(expression.getOperatorIdentifier());

        try {
            return operator.evaluate(elementData, operandValues);
        } catch (NoCodeException e) {
            throw new IllegalStateException(
                    "Failed to evaluate no-code operator '" + expression.getOperatorIdentifier() + "'",
                    e
            );
        }
    }

    @Nullable
    private Object resolveNodeData(
            @Nonnull NoCodeNodeDataReference nodeDataReference,
            @Nonnull ProcessExecutionData processDataContext
    ) {
        var allNodeData = processDataContext.get(ProcessExecutionData.NODE_RESULTS_KEY);
        if (!(allNodeData instanceof Map<?, ?> allNodeDataMap)) {
            return null;
        }

        var nodeDataKey = nodeDataReference.getNodeDataKey();
        if (nodeDataKey == null || nodeDataKey.isBlank()) {
            return null;
        }

        return allNodeDataMap.get(nodeDataKey);
    }

    @Nullable
    private static List<Integer> resolveWildcardIndices(@Nullable String path,
                                                        @Nullable List<Integer> wildcardIndices,
                                                        boolean allowArrayRoot) {
        var wildcardCount = ProcessDataValueUtils.countWildcardSegments(path, allowArrayRoot);
        if (wildcardCount == 0) {
            return null;
        }

        if (wildcardIndices == null) {
            throw new IllegalArgumentException("Wildcard destination keys require explicit indices.");
        }

        if (wildcardIndices.size() < wildcardCount) {
            throw new IllegalArgumentException(
                    "Wildcard destination key requires " + wildcardCount + " bound index values, but only " +
                            wildcardIndices.size() + " are available."
            );
        }

        return List.copyOf(wildcardIndices.subList(0, wildcardCount));
    }
}
