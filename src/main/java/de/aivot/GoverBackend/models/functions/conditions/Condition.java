package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class Condition {
    private ConditionOperator operator;
    private ConditionOperand operandA;
    private ConditionOperand operandB;
    private String conditionUnmetMessage;

    public Condition(Map<String, Object> data) {
        operator = MapUtils.getEnum(data, "operator", Integer.class, ConditionOperator.values());

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

        conditionUnmetMessage = MapUtils.getString(data, "conditionUnmetMessage");
    }

    public String evaluate(Map<String, Object> customerInput) {
        Object rawValA;
        if (operandA instanceof ConditionOperandReference op) {
            rawValA = customerInput.get(op.getId());
        } else {
            rawValA = ((ConditionOperandValue) operandA).getValue();
        }

        Object rawValB;
        if (operandB instanceof ConditionOperandReference op) {
            rawValB = customerInput.get(op.getId());
        } else {
            rawValB = ((ConditionOperandValue) operandB).getValue();
        }

        if (rawValA == null || rawValB == null) {
            return conditionUnmetMessage;
        }

        boolean conditionMet = false;

        if (rawValA instanceof String valA && rawValB instanceof String valB) {
            conditionMet = switch (operator) {
                case Equals -> valA.equals(valB);
                case EqualsIgnoreCase -> valA.equalsIgnoreCase(valB);
                case NotEquals -> !valA.equals(valB);
                case NotEqualsIgnoreCase -> !valA.equalsIgnoreCase(valB);

                case Includes -> valA.contains(valB);
                case NotIncludes -> !valA.contains(valB);

                case StartsWith -> valA.startsWith(valB);
                case NotStartsWith -> !valA.startsWith(valB);
                case EndsWith -> valA.endsWith(valB);
                case NotEndsWith -> !valA.endsWith(valB);

                case MatchesPattern -> {
                    try {
                        yield valA.matches("^" + valB + "$");
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case NotMatchesPattern -> {
                    try {
                        yield !valA.matches("^" + valB + "$");
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case IncludesPattern -> {
                    try {
                        yield valA.matches(valB);
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case NotIncludesPattern -> {
                    try {
                        yield !valA.matches(valB);
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }

                default -> false;
            };
        } else if (rawValA instanceof Integer valA && rawValB instanceof Integer valB) {
            conditionMet = switch (operator) {
                case Equals -> valA.equals(valB);
                case NotEquals -> !valA.equals(valB);

                case LessThan -> valA < valB;
                case LessThanOrEqual -> valA <= valB;
                case GreaterThan -> valA > valB;
                case GreaterThanOrEqual -> valA >= valB;

                default -> false;
            };
        } else if (rawValA instanceof Double valA && rawValB instanceof Double valB) {
            conditionMet = switch (operator) {
                case Equals -> valA.equals(valB);
                case NotEquals -> !valA.equals(valB);

                case LessThan -> valA < valB;
                case LessThanOrEqual -> valA <= valB;
                case GreaterThan -> valA > valB;
                case GreaterThanOrEqual -> valA >= valB;

                default -> false;
            };
        }

        return conditionMet ? null : conditionUnmetMessage;
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

    public String getConditionUnmetMessage() {
        return conditionUnmetMessage;
    }

    public void setConditionUnmetMessage(String conditionUnmetMessage) {
        this.conditionUnmetMessage = conditionUnmetMessage;
    }
}
