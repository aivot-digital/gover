package de.aivot.GoverBackend.models.functions.conditions;

import de.aivot.GoverBackend.enums.ConditionSetOperator;
import de.aivot.GoverBackend.models.elements.RootElement;
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
            List<Map<String, Object>> conditionsData = (List<Map<String, Object>>) data.get("conditions");
            if (conditionsData != null) {
                conditions = new LinkedList<>();
                for (Map<String, Object> conditionData : conditionsData) {
                    conditions.add(new Condition(conditionData));
                }
            }
        }
        if (data.containsKey("conditionsSets")) {
            List<Map<String, Object>> conditionSetsData = (List<Map<String, Object>>) data.get("conditionsSets");
            if (conditionSetsData != null) {
                conditionsSets = new LinkedList<>();
                for (Map<String, Object> conditionSetData : conditionSetsData) {
                    conditionsSets.add(new ConditionSet(conditionSetData));
                }
            }
        }
        conditionSetUnmetMessage = MapUtils.getString(data, "conditionSetUnmetMessage");
    }

    public ConditionSet(ConditionSetOperator operator, Collection<Condition> conditions, Collection<ConditionSet> conditionsSets, String conditionSetUnmetMessage) {
        this.operator = operator;
        this.conditions = conditions;
        this.conditionsSets = conditionsSets;
        this.conditionSetUnmetMessage = conditionSetUnmetMessage;
    }

    public String evaluate(String idPrefix, RootElement root, Map<String, Object> customerInput) {
        return switch (operator) {
            case All -> {
                if (conditions != null) {
                    for (var c : conditions) {
                        var res = c.evaluate(idPrefix, root, customerInput);
                        if (res != null) {
                            yield res;
                        }
                    }
                }

                if (conditionsSets != null) {
                    for (var c : conditionsSets) {
                        var res = c.evaluate(idPrefix, root, customerInput);
                        if (res != null) {
                            yield res;
                        }
                    }
                }

                yield null;
            }
            case Any -> {
                boolean res = (conditions != null && conditions.stream().anyMatch(c -> c.evaluate(idPrefix, root, customerInput) == null)) || (conditionsSets != null && conditionsSets.stream().anyMatch(cs -> cs.evaluate(idPrefix, root, customerInput) == null));
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
