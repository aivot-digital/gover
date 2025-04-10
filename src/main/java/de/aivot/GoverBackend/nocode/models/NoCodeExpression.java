package de.aivot.GoverBackend.nocode.models;

import de.aivot.GoverBackend.utils.MapUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import java.util.*;

/**
 * Represents an expression in the NoCode language.
 * An expression consists of an operator and a list of operands.
 */
public class NoCodeExpression implements NoCodeOperand {
    private final String operatorIdentifier;
    private final Collection<NoCodeOperand> operands;

    public NoCodeExpression(Map<String, Object> values) {
        operatorIdentifier = MapUtils.getString(values, "operatorIdentifier");
        operands = MapUtils
                .getCollectionKeepNull(values, "operands", (operandValue) -> {
                    if (operandValue == null) {
                        return null;
                    } else if (operandValue.containsKey("value")) {
                        return new NoCodeStaticValue(operandValue.get("value"));
                    } else if (operandValue.containsKey("elementId")) {
                        return new NoCodeReference((String) operandValue.get("elementId"));
                    } else {
                        return new NoCodeExpression(operandValue);
                    }
                });
    }

    public NoCodeExpression(String operatorIdentifier, NoCodeOperand... operands) {
        this.operatorIdentifier = operatorIdentifier;
        this.operands = Arrays.stream(operands).toList();
    }

    public String getOperatorIdentifier() {
        return operatorIdentifier;
    }

    /**
     * This collection may contain null values, which represent null operands.
     *
     * @return The operands of the expression.
     */
    public Collection<NoCodeOperand> getOperands() {
        return operands;
    }

    public boolean isEmpty() {
        return StringUtils.isNullOrEmpty(operatorIdentifier) && operands.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoCodeExpression that = (NoCodeExpression) o;
        return Objects.equals(operatorIdentifier, that.operatorIdentifier) && Objects.equals(operands, that.operands);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(operatorIdentifier);
        result = 31 * result + operands.hashCode();
        return result;
    }

    public Set<String> getReferencedIds() {
        if (operands == null || operands.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> referencedIds = new HashSet<>();
        for (NoCodeOperand operand : operands) {
            if (operand != null) {
                switch (operand) {
                    case NoCodeReference reference -> referencedIds.add(reference.getElementId());
                    case NoCodeExpression expression -> referencedIds.addAll(expression.getReferencedIds());
                    default -> {
                    }
                }
            }
        }

        return referencedIds;
    }
}
