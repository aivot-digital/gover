package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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

    public String evaluate(RootElement root, Map<String, Object> customerInput) {
        if (operator == null) {
            return "Evaluation failed. No operator";
        }

        if (reference == null) {
            return "Evaluation failed. reference";
        }

        Optional<? extends BaseElement> optReferencedElement = root.findChild(reference);

        if (optReferencedElement.isEmpty()) {
            return conditionUnmetMessage != null ? conditionUnmetMessage : "Referenced element not found";
        }

        BaseElement referencedElement = optReferencedElement.get();
        Object rawValA = customerInput.get(referencedElement.getId());

        Object rawValB = null;
        if (!operator.getUnary()) {
            if (target != null) {
                rawValB = customerInput.get(target);
            } else {
                rawValB = value;
            }
        }

        boolean evaluationResult = referencedElement.evaluate(operator, rawValA, rawValB);
        if (evaluationResult) {
            return null;
        } else {
            return conditionUnmetMessage != null ? conditionUnmetMessage : "Evaluation returned false";
        }

        /*
        boolean conditionMet = switch (operator) {
            case Equals -> compareEquals(rawValA, rawValB);
            case NotEquals -> {
            }
            case LessThan -> {
            }
            case LessThanOrEqual -> {
            }
            case GreaterThan -> {
            }
            case GreaterThanOrEqual -> {
            }
            case Includes -> {
            }
            case NotIncludes -> {
            }
            case StartsWith -> {
            }
            case NotStartsWith -> {
            }
            case EndsWith -> {
            }
            case NotEndsWith -> {
            }
            case MatchesPattern -> {
            }
            case NotMatchesPattern -> {
            }
            case IncludesPattern -> {
            }
            case NotIncludesPattern -> {
            }
            case EqualsIgnoreCase -> {
            }
            case NotEqualsIgnoreCase -> {
            }
            case Empty -> {
            }
            case NotEmpty -> {
            }
        };

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

                case Empty -> valA.isEmpty();
                case NotEmpty -> !valA.isEmpty();

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
        } else if (rawValA instanceof Boolean valA) {
            boolean valB = Boolean.FALSE;
            if (rawValB instanceof Boolean) {
                valB = (Boolean) rawValB;
            } else if (rawValB instanceof String) {
                valB = "Ja (True)".equals(rawValB);
            }

            conditionMet = switch (operator) {
                case Equals -> valA == valB;
                case NotEquals -> valA != valB;

                case Empty -> !valA;
                case NotEmpty -> valA;

                default -> false;
            };
        }

        return conditionMet ? null : conditionUnmetMessage;

         */
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
