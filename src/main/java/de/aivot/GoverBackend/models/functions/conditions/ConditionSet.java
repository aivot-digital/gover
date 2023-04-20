package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionSetOperator;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConditionSet {
    private ConditionSetOperator operator;
    private Collection<Condition> conditions;
    private Collection<ConditionSet> conditionsSets;
    private String conditionSetUnmetMessage;

    public ConditionSet(Map<String, Object> data) {
        operator = MapUtils.getEnum(data, "operator", Integer.class, ConditionSetOperator.values());
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
        conditionSetUnmetMessage = MapUtils.getString(data, "conditionSetUnmetMessage");
    }

    public String evaluate(Map<String, Object> customerInput) {
        return switch (operator) {
            case All -> {
                for (var c : conditions) {
                    var res = c.evaluate(customerInput);
                    if (res != null) {
                        yield res;
                    }
                }

                for (var c : conditionsSets) {
                    var res = c.evaluate(customerInput);
                    if (res != null) {
                        yield res;
                    }
                }

                yield null;
            }
            case Any -> {
                boolean res = conditions.stream().anyMatch(c -> c.evaluate(customerInput) == null) && conditionsSets.stream().anyMatch(cs -> cs.evaluate(customerInput) == null);
                if (!res) {
                    yield conditionSetUnmetMessage;
                }
                yield null;
            }
        };
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
}
