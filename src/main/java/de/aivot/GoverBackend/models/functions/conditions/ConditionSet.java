package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionSetOperator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConditionSet {
    private ConditionSetOperator operator;
    private Collection<Condition> conditions;
    private Collection<ConditionSet> conditionsSets;

    public ConditionSet(Map<String, Object> data) {
        operator = ConditionSetOperator.findElement(data.get("operator")).orElse(null);
        if (data.containsKey("conditions")) {
            conditions = new LinkedList<>();
            List<Map<String, Object>> conditionsData = (List<Map<String, Object>>) data.get("conditions");
            for (Map<String, Object> conditionData : conditionsData) {
                conditions.add(new Condition(conditionData));
            }
        }
        if (data.containsKey("conditionSets")) {
            conditionsSets = new LinkedList<>();
            List<Map<String, Object>> conditionSetsData = (List<Map<String, Object>>) data.get("conditionSets");
            for (Map<String, Object> conditionSetData : conditionSetsData) {
                conditionsSets.add(new ConditionSet(conditionSetData));
            }
        }
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
}
