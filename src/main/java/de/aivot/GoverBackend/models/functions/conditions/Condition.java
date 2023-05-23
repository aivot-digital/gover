package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class Condition {
    private ConditionOperator operator;
    private String reference;
    private String target;
    private String value;
    private String conditionUnmetMessage;

    public Condition(Map<String, Object> data) {
        operator = MapUtils.getEnum(data, "operator", Integer.class, ConditionOperator.values());

        reference = MapUtils.getString(data, "reference");
        target = MapUtils.getString(data, "target");
        value = MapUtils.getString(data, "value");

        conditionUnmetMessage = MapUtils.getString(data, "conditionUnmetMessage");
    }

    public String evaluate(Map<String, Object> customerInput) {
        Object rawValA = customerInput.get(reference);

        Object rawValB;
        if (target != null) {
            rawValB = customerInput.get(target);
        } else {
            rawValB = value;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getConditionUnmetMessage() {
        return conditionUnmetMessage;
    }

    public void setConditionUnmetMessage(String conditionUnmetMessage) {
        this.conditionUnmetMessage = conditionUnmetMessage;
    }
}
