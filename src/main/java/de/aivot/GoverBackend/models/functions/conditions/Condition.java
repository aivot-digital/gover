package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;

import java.util.Map;

public class Condition {
    private ConditionOperator operator;
    private ConditionOperand operandA;
    private ConditionOperand operandB;

    public Condition(Map<String, Object> data) {
        operator = ConditionOperator.findElement(data.get("operator")).orElse(null);

        if (data.get("operandA") != null) {
            Map<String, Object> operandAData = (Map<String, Object>) data.get("operandA");
            if (operandAData.containsKey("value")) {
                operandA = new ConditionOperandValue(operandAData);
            } else {
                operandA = new ConditionOperandReference(operandAData);
            }
        }

        if (data.get("operandB") != null) {
            Map<String, Object> operandBData = (Map<String, Object>) data.get("operandB");
            if (operandBData.containsKey("value")) {
                operandB = new ConditionOperandValue(operandBData);
            } else {
                operandB = new ConditionOperandReference(operandBData);
            }
        }
    }

    public boolean evaluate(Map<String, Object> customerInput) {
        Object valA = operandA instanceof ConditionOperandReference ? ((ConditionOperandReference) operandA).getId() : ((ConditionOperandValue) operandA).getValue();
        Object valB = operandA instanceof ConditionOperandReference ? ((ConditionOperandReference) operandA).getId() : ((ConditionOperandValue) operandA).getValue();

        if (valA == null || valB == null) {
            return false;
        }

        if (valA instanceof String && valB instanceof String) {

        } else if ((valA instanceof Integer && valB instanceof Integer) || (valA instanceof Double && valB instanceof Double)) {

        }

        return switch (operator) {
            case Equals -> valA.equals(valB);
            case NotEquals -> !valA.equals(valB);

            case LessThan -> Double.valueOf(valA) <= valB;
            case LessThanOrEqual -> valA valB;
            case GreaterThan -> valA valB;
            case GreaterThanOrEqual -> valA valB;

            case Includes -> valA valB;
            case NotIncludes -> valA valB;
            case StartsWith -> valA valB;
            case NotStartsWith -> valA valB;
            case EndsWith -> valA valB;
            case NotEndsWith -> valA valB;
            case MatchesPattern -> valA valB;
            case NotMatchesPattern -> valA valB;
            case IncludesPattern -> valA valB;
            case NotIncludesPattern -> valA valB;
            default -> false;
        };
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }

    public ConditionOperand getOperandA() {
        return operandA;
    }

    public void setOperandA(ConditionOperand operandA) {
        this.operandA = operandA;
    }

    public ConditionOperand getOperandB() {
        return operandB;
    }

    public void setOperandB(ConditionOperand operandB) {
        this.operandB = operandB;
    }
}
