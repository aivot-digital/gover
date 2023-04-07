package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;

import java.util.Map;

public class FunctionNoCode extends Function {
    private ConditionSet conditionSet;

    public FunctionNoCode(Map<String, Object> data) {
        super(data);
        Map<String, Object> conditionSetData = (Map<String, Object>) data.get("conditionSet");
        if (conditionSetData != null) {
            conditionSet = new ConditionSet(conditionSetData);
        }
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }
}
