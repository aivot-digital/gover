package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service evaluates no code expressions.
 * It uses the operators provided by the registered {@link NoCodeOperatorsProvider} to evaluate the expressions.
 */
@Service
public class NoCodeEvaluationService {
    private static final Logger logger = LoggerFactory.getLogger(NoCodeEvaluationService.class);
    private static final Pattern PATH_TOKEN_PATTERN = Pattern.compile("([^.\\[\\]]+)|\\[(\\d+)]");

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
    public NoCodeResult evaluate(@Nullable NoCodeOperand operand, @Nonnull ElementData elementData) {
        return evaluate(operand, elementData, Map.of());
    }

    @Nonnull
    public NoCodeResult evaluate(
            @Nullable NoCodeOperand operand,
            @Nonnull ElementData elementData,
            @Nonnull Map<String, Object> processDataContext
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
                var dataObject = elementId == null ? null : elementData.get(elementId);
                var value = dataObject == null
                        ? null
                        : dataObject.getValue();
                yield new NoCodeResult(value);
            }

            // Process data references resolve based on the injected process context map
            case NoCodeProcessDataReference processDataReference -> {
                var sourceData = processDataContext.get("$");
                var value = resolvePath(sourceData, processDataReference.getPath());
                yield new NoCodeResult(value);
            }

            case NoCodeInstanceDataReference instanceDataReference -> {
                var sourceData = processDataContext.get("$$");
                var value = resolvePath(sourceData, instanceDataReference.getPath());
                yield new NoCodeResult(value);
            }

            case NoCodeNodeDataReference nodeDataReference -> {
                var sourceData = resolveNodeData(nodeDataReference, processDataContext);
                var value = resolvePath(sourceData, nodeDataReference.getPath());
                yield new NoCodeResult(value);
            }

            // Expressions resolve by evaluating them
            case NoCodeExpression expression -> evaluateNoCodeExpression(expression, elementData, processDataContext);

            // Unknown operands are not supported
            default -> throw new IllegalStateException("Unexpected value: " + operand);
        };
    }

    @Nonnull
    private NoCodeResult evaluateNoCodeExpression(
            @Nonnull NoCodeExpression expression,
            @Nonnull ElementData elementData,
            @Nonnull Map<String, Object> processDataContext
    ) {
        var operands = expression.getOperands();
        if (operands == null) {
            operands = List.of();
        }

        var operandValues = operands
                .stream()
                .map(op -> evaluate(op, elementData, processDataContext))
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
            @Nonnull Map<String, Object> processDataContext
    ) {
        var allNodeData = processDataContext.get("_");
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
    private Object resolvePath(@Nullable Object root, @Nullable String path) {
        if (root == null) {
            return null;
        }

        if (path == null || path.isBlank()) {
            return root;
        }

        var tokens = tokenizePath(path);
        var current = root;
        for (var token : tokens) {
            if (current == null) {
                return null;
            }

            if (token.index != null) {
                if (current instanceof List<?> list) {
                    if (token.index < 0 || token.index >= list.size()) {
                        return null;
                    }
                    current = list.get(token.index);
                } else {
                    return null;
                }
            } else if (token.key != null) {
                if (current instanceof Map<?, ?> map) {
                    current = map.get(token.key);
                } else {
                    return null;
                }
            }
        }

        return current;
    }

    @Nonnull
    private List<PathToken> tokenizePath(@Nonnull String path) {
        var matcher = PATH_TOKEN_PATTERN.matcher(path);
        var tokens = new ArrayList<PathToken>();
        while (matcher.find()) {
            var key = matcher.group(1);
            var index = matcher.group(2);
            if (key != null) {
                tokens.add(new PathToken(key, null));
            } else if (index != null) {
                tokens.add(new PathToken(null, Integer.parseInt(index)));
            }
        }
        return tokens;
    }

    private record PathToken(@Nullable String key, @Nullable Integer index) {
    }
}
