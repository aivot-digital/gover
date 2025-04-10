package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
     * @param exp     The expression to evaluate
     * @param context The form state, containing the values and visibilities of the elements
     * @return The result of the evaluation
     */
    @Nonnull
    public NoCodeResult evaluate(@Nonnull NoCodeExpression exp, @Nonnull ElementDerivationData context, @Nullable String idPrefix) {
        var operands = exp.getOperands();
        if (operands == null) {
            operands = List.of();
        }

        var operandValues = operands
                .stream()
                .map(op -> resolveNoCodeOperand(op, context, idPrefix))
                .toArray();

        var operator = getNoCodeOperator(exp.getOperatorIdentifier());

        try {
            return operator.evaluate(context, operandValues);
        } catch (NoCodeException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private Object resolveNoCodeOperand(@Nullable NoCodeOperand operand, @Nonnull ElementDerivationData context, @Nullable String idPrefix) {
        if (operand == null) {
            return null;
        }

        return switch (operand) {
            // Static values resolve directly
            case NoCodeStaticValue staticValue -> staticValue.getValue();

            // Referenced elements resolve based on their visibility and the values map
            case NoCodeReference reference -> context
                    .getValue(idPrefix != null ? idPrefix + "_" + reference.getElementId() : reference.getElementId())
                    .orElse(null);

            // Expressions resolve by evaluating them
            case NoCodeExpression expression -> evaluate(expression, context, idPrefix).getValue();

            // Unknown operands are not supported
            default -> throw new IllegalStateException("Unexpected value: " + operand);
        };
    }
}
