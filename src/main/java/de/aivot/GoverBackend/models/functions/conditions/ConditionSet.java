package de.aivot.GoverBackend.models.functions.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.enums.ConditionSetOperator;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.*;

public class ConditionSet {
    private ConditionSetOperator operator;
    private Collection<Condition> conditions;
    private Collection<ConditionSet> conditionsSets;
    private String conditionSetUnmetMessage;

    public ConditionSet() {

    }

    public ConditionSet(ConditionSetOperator operator, Collection<Condition> conditions, Collection<ConditionSet> conditionsSets, String conditionSetUnmetMessage) {
        this.operator = operator;
        this.conditions = conditions;
        this.conditionsSets = conditionsSets;
        this.conditionSetUnmetMessage = conditionSetUnmetMessage;
    }

    public String evaluate(ElementWithChildren<?> rootElement, ElementData elementData, BaseElement element) {
        return switch (operator) {
            case All -> evaluateAll(rootElement, elementData, element);
            case Any -> evaluateAny(rootElement, elementData, element);
        };
    }

    /**
     * Evaluates if all conditions and condition sets return no error message.
     * Returns NULL if no error message was returned, otherwise returns the occurred error message.
     */
    private String evaluateAll(ElementWithChildren<?> rootElement, ElementData elementData, BaseElement element) {
        if (conditions != null) {
            for (var condition : conditions) {
                var conditionResult = condition.evaluate(rootElement, elementData, element);
                if (conditionResult != null) {
                    return conditionResult;
                }
            }
        }

        if (conditionsSets != null) {
            for (var conditionSet : conditionsSets) {
                var conditionSerResult = conditionSet.evaluate(rootElement, elementData, element);
                if (conditionSerResult != null) {
                    return conditionSerResult;
                }
            }
        }

        return null;
    }

    /**
     * Evaluates if at least one condition or condition set returns no error message.
     * Returns NULL at least one condition or condition set returns no error message, otherwise returns the condition set unmet message.
     */
    private String evaluateAny(ElementWithChildren<?> rootElement, ElementData elementData, BaseElement element) {
        if (conditions != null) {
            for (var condition : conditions) {
                var conditionResult = condition.evaluate(rootElement, elementData, element);
                if (conditionResult == null) {
                    return null;
                }
            }
        }

        if (conditionsSets != null) {
            for (var conditionSet : conditionsSets) {
                var conditionSetResult = conditionSet.evaluate(rootElement, elementData, element);
                if (conditionSetResult == null) {
                    return null;
                }
            }
        }

        return conditionSetUnmetMessage != null ? conditionSetUnmetMessage : "No condition or condition set returned no error message";
/*
                boolean res = (conditions != null && conditions.stream().anyMatch(c -> c.evaluate(idPrefix, root, customerInput, scriptEngine) == null)) || (conditionsSets != null && conditionsSets.stream().anyMatch(cs -> cs.evaluate(idPrefix, root, customerInput, scriptEngine) == null));
                if (!res) {
                    yield conditionSetUnmetMessage;
                }
                yield null;

 */
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConditionSet that = (ConditionSet) o;

        if (operator != that.operator) return false;
        if (!Objects.equals(conditions, that.conditions)) return false;
        if (!Objects.equals(conditionsSets, that.conditionsSets))
            return false;
        return Objects.equals(conditionSetUnmetMessage, that.conditionSetUnmetMessage);
    }

    @Override
    public int hashCode() {
        int result = operator != null ? operator.hashCode() : 0;
        result = 31 * result + (conditions != null ? conditions.hashCode() : 0);
        result = 31 * result + (conditionsSets != null ? conditionsSets.hashCode() : 0);
        result = 31 * result + (conditionSetUnmetMessage != null ? conditionSetUnmetMessage.hashCode() : 0);
        return result;
    }

    public ConditionSetOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionSetOperator operator) {
        this.operator = operator;
    }

    public Collection<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Collection<Condition> conditions) {
        this.conditions = conditions;
    }

    public Collection<ConditionSet> getConditionsSets() {
        return conditionsSets;
    }

    public void setConditionsSets(Collection<ConditionSet> conditionsSets) {
        this.conditionsSets = conditionsSets;
    }

    public String getConditionSetUnmetMessage() {
        return conditionSetUnmetMessage;
    }

    public void setConditionSetUnmetMessage(String conditionSetUnmetMessage) {
        this.conditionSetUnmetMessage = conditionSetUnmetMessage;
    }

    @JsonIgnore
    public Set<String> getReferencedIds() {
        var referencedIds = new HashSet<String>();

        if (conditions != null) {
            for (var condition : conditions) {
                if (condition.getReference() != null) {
                    referencedIds.add(condition.getReference());
                }
                if (condition.getTarget() != null) {
                    referencedIds.add(condition.getTarget());
                }
            }
        }

        if (conditionsSets != null) {
            for (var conditionSet : conditionsSets) {
                referencedIds.addAll(conditionSet.getReferencedIds());
            }
        }

        return referencedIds;
    }
}
