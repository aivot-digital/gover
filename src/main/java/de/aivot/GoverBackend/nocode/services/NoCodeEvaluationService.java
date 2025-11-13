package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service evaluates no code expressions.
 * It uses the operators provided by the registered {@link NoCodeOperatorServiceProvider} to evaluate the expressions.
 */
@Service
public class NoCodeEvaluationService {
    private static final Logger logger = LoggerFactory.getLogger(NoCodeEvaluationService.class);

    private final Map<String, NoCodeOperator> noCodeOperatorProviders;

    @Autowired
    public NoCodeEvaluationService(List<NoCodeOperatorServiceProvider> noCodeOperatorProviders) {
        this.noCodeOperatorProviders = new HashMap<>();

        for (var provider : noCodeOperatorProviders) {
            for (var operator : provider.getOperators()) {
                var packageScopedOperatorIdentifier = provider.getPackageName() + "." + operator.getIdentifier();
                if (this.noCodeOperatorProviders.containsKey(packageScopedOperatorIdentifier)) {
                    logger
                            .atWarn()
                            .setMessage("No code operator with identifier " + packageScopedOperatorIdentifier + " already exists. Skipping this operator.")
                            .addKeyValue("operatorIdentifier", packageScopedOperatorIdentifier)
                            .addKeyValue("providerClassName", provider.getClass().getName())
                            .addKeyValue("providerPackageName", provider.getPackageName())
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
    private NoCodeOperator getNoCodeOperator(String identifier) {
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
                var value = elementData
                    .get(reference.getElementId())
                    .getValue();
                yield new NoCodeResult(value);
            }

            // Expressions resolve by evaluating them
            case NoCodeExpression expression -> evaluateNoCodeExpression(expression, elementData);

            // Unknown operands are not supported
            default -> throw new IllegalStateException("Unexpected value: " + operand);
        };
    }

    @Nonnull
    private NoCodeResult evaluateNoCodeExpression(@Nonnull NoCodeExpression expression, @Nonnull ElementData elementData) {
        var operands = expression.getOperands();
        if (operands == null) {
            operands = List.of();
        }

        var operandValues = operands
                .stream()
                .map(op -> evaluate(op, elementData))
                .map(NoCodeResult::getValue)
                .toArray();

        var operator = getNoCodeOperator(expression.getOperatorIdentifier());

        try {
            return operator.evaluate(elementData, operandValues);
        } catch (NoCodeException e) {
            throw new RuntimeException(e);
        }
    }
}
