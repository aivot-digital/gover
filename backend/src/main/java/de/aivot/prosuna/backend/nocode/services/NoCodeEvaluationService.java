package de.aivot.prosuna.backend.nocode.services;

import java.util.function.UnaryOperator;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDryRunStatus;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.*;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIfOperator;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This service evaluates no code expressions. It uses the operators provided by the registered {@link NoCodeOperatorsProvider} to evaluate the expressions.
 */
@Service
public class NoCodeEvaluationService {
    private static final Logger logger = LoggerFactory.getLogger(NoCodeEvaluationService.class);

    private final Map<String, NoCodeOperator> noCodeOperatorProviders;
    private final Set<String> dryRunOperatorIdentifiers = new HashSet<>();

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
                    if (provider.supportsDryRun(operator)) {
                        dryRunOperatorIdentifiers.add(packageScopedOperatorIdentifier);
                    }
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
            case NoCodeStaticValue staticValue -> new NoCodeResult(resolveStaticValue(staticValue, null));

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

    /**
     * Evaluates an operand like {@link #evaluate} and keeps the result of every nested operand, so the editor can show intermediate results. Failures are reported per operand
     * instead of aborting the evaluation, and operators whose provider does not support dry runs are skipped, together with every expression that depends on them.
     *
     * @param operand            The operand to evaluate
     * @param elementData        The test values and states of the referenced elements
     * @param processDataContext The test process data
     * @return The result of the operand, with the results of its nested operands
     */
    @Nonnull
    public NoCodeDryRunResultDTO dryRun(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext
    ) {
        return dryRun(operand, elementData, processDataContext, UnaryOperator.identity());
    }

    /**
     * Apply the response budget to every intermediate result before passing it to a parent operator.
     */
    @Nonnull
    public NoCodeDryRunResultDTO dryRun(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nonnull UnaryOperator<NoCodeDryRunResultDTO> limitResult
    ) {
        return limitResult.apply(evaluateDryRun(operand, elementData, processDataContext, limitResult, null));
    }

    @Nonnull
    private NoCodeDryRunResultDTO evaluateDryRun(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nonnull UnaryOperator<NoCodeDryRunResultDTO> limitResult,
            @Nullable NoCodeParameter inputParameter
    ) {
        if (!(operand instanceof NoCodeExpression expression)) {
            try {
                var value = operand instanceof NoCodeStaticValue staticValue
                        ? resolveStaticValue(staticValue, inputParameter)
                        : evaluate(operand, elementData, processDataContext).getValue();
                return NoCodeDryRunResultDTO.evaluated(value, List.of());
            } catch (RuntimeException e) {
                logger.debug("No-code dry run could not resolve an operand", e);
                return NoCodeDryRunResultDTO.failed("Der Wert konnte nicht gelesen werden.", List.of());
            }
        }

        var operands = expression.getOperands();
        if (operands == null) operands = List.of();
        var operatorIdentifier = expression.getOperatorIdentifier();
        var operator = operatorIdentifier == null ? null : noCodeOperatorProviders.get(operatorIdentifier);
        var signature = findWhitespaceSignature(operator, operands);
        var dryRunAllowed = dryRunOperatorIdentifiers.contains(operatorIdentifier);
        if (dryRunAllowed && operator instanceof NoCodeIfOperator conditional && operands.size() == 3) {
            return evaluateIfDryRun(conditional, new ArrayList<>(operands), elementData, processDataContext, limitResult, signature);
        }
        var decisiveValue = shortCircuitValue(operator, operands.size());
        if (dryRunAllowed && decisiveValue != null) {
            return evaluateBooleanDryRun(operator, operands, decisiveValue, elementData, processDataContext, limitResult, signature);
        }
        var childResults = new ArrayList<NoCodeDryRunResultDTO>(operands.size());
        int index = 0;
        for (var child : operands) {
            var childResult = evaluateDryRun(child, elementData, processDataContext, limitResult, parameterAt(signature, index));
            childResults.add(limitResult.apply(childResult));
            index++;
        }
        var operandResults = List.copyOf(childResults);
        if (operator == null) {
            return NoCodeDryRunResultDTO.failed("Die Funktion ist nicht verfügbar.", operandResults);
        }
        if (!dryRunAllowed) {
            return NoCodeDryRunResultDTO.skipped(
                    "Diese Funktion wird im Testlauf nicht ausgeführt, weil sie geschützte Daten liest oder Aktionen auslösen kann.",
                    operandResults
            );
        }
        if (operandResults.stream().anyMatch(result -> result.status() != NoCodeDryRunStatus.Evaluated)) {
            return NoCodeDryRunResultDTO.skipped(
                    "Ein Parameter konnte nicht ausgewertet werden.",
                    operandResults
            );
        }

        var operandValues = operandResults.stream().map(NoCodeDryRunResultDTO::value).toArray();
        try {
            return NoCodeDryRunResultDTO.evaluated(operator.evaluate(elementData, operandValues).getValue(), operandResults);
        } catch (NoCodeWrongArgumentCountException e) {
            return NoCodeDryRunResultDTO.failed("Die Anzahl der Parameter passt nicht zur Funktion.", operandResults);
        } catch (NoCodeException e) {
            // Operators raise NoCodeException with messages written for the people who author expressions.
            return NoCodeDryRunResultDTO.failed(
                    e.getMessage() == null ? "Die Funktion konnte nicht ausgewertet werden." : e.getMessage(),
                    operandResults
            );
        } catch (RuntimeException e) {
            logger.debug("No-code dry run failed for operator {}", operatorIdentifier, e);
            return NoCodeDryRunResultDTO.failed("Die Funktion konnte mit diesen Werten nicht ausgewertet werden.", operandResults);
        }
    }

    @Nonnull
    private NoCodeDryRunResultDTO evaluateIfDryRun(
            @Nonnull NoCodeIfOperator operator,
            @Nonnull List<NoCodeOperand> operands,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nonnull UnaryOperator<NoCodeDryRunResultDTO> limitResult,
            @Nullable NoCodeSignatur signature
    ) {
        var condition = limitResult.apply(evaluateDryRun(
                operands.getFirst(), elementData, processDataContext, limitResult, parameterAt(signature, 0)));
        if (condition.status() != NoCodeDryRunStatus.Evaluated) {
            var message = "Die Bedingung konnte nicht ausgewertet werden.";
            var skipped = NoCodeDryRunResultDTO.skipped(message, List.of());
            return NoCodeDryRunResultDTO.skipped(message,
                    List.of(condition, limitResult.apply(skipped), limitResult.apply(skipped)));
        }

        var selectedIndex = operator.selectedResultIndex(condition.value());
        var selected = limitResult.apply(evaluateDryRun(
                operands.get(selectedIndex), elementData, processDataContext, limitResult, parameterAt(signature, selectedIndex)));
        var conditionWasTrue = selectedIndex == 1;
        // Keep operand positions aligned with the editor tree. The unused subtree has no child results.
        var skipped = limitResult.apply(NoCodeDryRunResultDTO.skipped(
                conditionWasTrue ? "Die Bedingung ist wahr." : "Die Bedingung ist falsch.", List.of()));
        var results = conditionWasTrue ? List.of(condition, selected, skipped) : List.of(condition, skipped, selected);
        return selected.status() == NoCodeDryRunStatus.Evaluated
                ? NoCodeDryRunResultDTO.evaluated(selected.value(), results)
                : NoCodeDryRunResultDTO.skipped("Das gewählte Ergebnis konnte nicht ausgewertet werden.", results);
    }

    @Nonnull
    private NoCodeDryRunResultDTO evaluateBooleanDryRun(
            @Nonnull NoCodeOperator operator,
            @Nonnull Collection<NoCodeOperand> operands,
            boolean decisiveValue,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nonnull UnaryOperator<NoCodeDryRunResultDTO> limitResult,
            @Nullable NoCodeSignatur signature
    ) {
        var results = new ArrayList<NoCodeDryRunResultDTO>(operands.size());
        for (var operand : operands) {
            var index = results.size();
            var condition = limitResult.apply(evaluateDryRun(
                    operand, elementData, processDataContext, limitResult, parameterAt(signature, index)));
            results.add(condition);
            if (condition.status() != NoCodeDryRunStatus.Evaluated) {
                var message = switch (operator) {
                    case NoCodeAndOperator ignored -> index == 0
                            ? "Die erste Bedingung konnte nicht ausgewertet werden."
                            : "Die zweite Bedingung konnte nicht ausgewertet werden.";
                    default -> "Bedingung " + (index + 1) + " konnte nicht ausgewertet werden.";
                };
                appendSkippedConditions(results, operands.size(), limitResult, message);
                return NoCodeDryRunResultDTO.skipped(message, List.copyOf(results));
            }
            if (operator.castToBoolean(condition.value()) == decisiveValue) {
                var message = decisiveValue ? "Bedingung " + (index + 1) + " ist wahr." : "Die erste Bedingung ist falsch.";
                appendSkippedConditions(results, operands.size(), limitResult, message);
                return NoCodeDryRunResultDTO.evaluated(decisiveValue, List.copyOf(results));
            }
        }
        return NoCodeDryRunResultDTO.evaluated(!decisiveValue, List.copyOf(results));
    }

    private static void appendSkippedConditions(
            @Nonnull List<NoCodeDryRunResultDTO> results,
            int operandCount,
            @Nonnull UnaryOperator<NoCodeDryRunResultDTO> limitResult,
            @Nonnull String message
    ) {
        // The editor maps results by position. The skipped conditions have no nested results.
        while (results.size() < operandCount) {
            results.add(limitResult.apply(NoCodeDryRunResultDTO.skipped(message, List.of())));
        }
    }

    @Nullable
    private static Boolean shortCircuitValue(@Nullable NoCodeOperator operator, int operandCount) {
        return switch (operator) {
            case NoCodeAndOperator ignored when operandCount == 2 -> false;
            case NoCodeOrOperator ignored when NoCodeOrOperator.supportsConditionCount(operandCount) -> true;
            case null, default -> null;
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
        if (operands == null) operands = List.of();
        var registeredOperator = noCodeOperatorProviders.get(expression.getOperatorIdentifier());
        var signature = findWhitespaceSignature(registeredOperator, operands);
        if (registeredOperator instanceof NoCodeIfOperator conditional && operands.size() == 3) {
            var inputs = new ArrayList<>(operands);
            var condition = evaluateArgument(inputs.getFirst(), elementData, processDataContext, wildcardIndices, parameterAt(signature, 0));
            var selectedIndex = conditional.selectedResultIndex(condition);
            return new NoCodeResult(evaluateArgument(
                    inputs.get(selectedIndex), elementData, processDataContext, wildcardIndices, parameterAt(signature, selectedIndex)));
        }
        var decisiveValue = shortCircuitValue(registeredOperator, operands.size());
        if (decisiveValue != null) {
            int index = 0;
            for (var operand : operands) {
                var value = evaluateArgument(operand, elementData, processDataContext, wildcardIndices, parameterAt(signature, index));
                if (registeredOperator.castToBoolean(value) == decisiveValue.booleanValue()) {
                    return new NoCodeResult(decisiveValue);
                }
                index++;
            }
            return new NoCodeResult(!decisiveValue);
        }
        var operandValues = new Object[operands.size()];
        int index = 0;
        for (var operand : operands) {
            operandValues[index] = evaluateArgument(operand, elementData, processDataContext, wildcardIndices, parameterAt(signature, index));
            index++;
        }
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
    private Object evaluateArgument(
            @Nullable NoCodeOperand operand,
            @Nonnull DerivedRuntimeElementData elementData,
            @Nonnull ProcessExecutionData processDataContext,
            @Nullable List<Integer> wildcardIndices,
            @Nullable NoCodeParameter parameter
    ) {
        return operand instanceof NoCodeStaticValue staticValue
                ? resolveStaticValue(staticValue, parameter)
                : evaluate(operand, elementData, processDataContext, wildcardIndices).getValue();
    }

    @Nullable
    private static Object resolveStaticValue(@Nonnull NoCodeStaticValue operand, @Nullable NoCodeParameter parameter) {
        var value = operand.getValue();
        // Keep the literal in stored JSON. Only evaluation decides whether whitespace means a missing value.
        if (value instanceof String text && StringUtils.isNullOrEmpty(text)) {
            return !text.isEmpty() && parameter != null && Boolean.TRUE.equals(parameter.preserveWhitespace()) ? text : null;
        }
        return value;
    }

    @Nullable
    private static NoCodeSignatur findWhitespaceSignature(
            @Nullable NoCodeOperator operator,
            @Nonnull Collection<NoCodeOperand> operands
    ) {
        if (operator == null || operands.stream().noneMatch(operand -> operand instanceof NoCodeStaticValue staticValue
                && staticValue.getValue() instanceof String text && !text.isEmpty() && StringUtils.isNullOrEmpty(text))) {
            return null;
        }
        var signatures = operator.getSignatures();
        if (signatures == null) return null;

        // Use the same exact-then-longest match as the editor for variadic inputs.
        NoCodeSignatur variadicMatch = null;
        for (var signature : signatures) {
            int count = signature.parameters().length;
            if (count == operands.size()) return signature;
            if (count < operands.size() && operator.hasVariableArgumentCount()
                    && (variadicMatch == null || count > variadicMatch.parameters().length)) {
                variadicMatch = signature;
            }
        }
        return variadicMatch;
    }

    @Nullable
    private static NoCodeParameter parameterAt(@Nullable NoCodeSignatur signature, int index) {
        if (signature == null || signature.parameters().length == 0) return null;
        return signature.parameters()[Math.min(index, signature.parameters().length - 1)];
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
