package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;

import javax.script.ScriptEngine;
import java.util.Map;

public class FunctionNoCode<T> extends Function<T> {
    private ConditionSet conditionSet;

    public FunctionNoCode(Map<String, Object> data) {
        super(data);
        Map<String, Object> conditionSetData = (Map<String, Object>) data.get("conditionSet");
        if (conditionSetData != null) {
            conditionSet = new ConditionSet(conditionSetData);
        }
    }

    @Override
    public T evaluate(BaseElement element, Map<String, Object> customerInput, String id, ScriptEngine scriptEngine) {
        // TODO
        return null;
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }
}
