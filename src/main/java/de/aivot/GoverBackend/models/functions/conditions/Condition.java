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
