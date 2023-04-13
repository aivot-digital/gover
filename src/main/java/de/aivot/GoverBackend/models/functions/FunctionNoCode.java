package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Map;

public class FunctionNoCode extends Function {
    private ConditionSet conditionSet;

    public FunctionNoCode(Map<String, Object> data) {
        super(data);
        conditionSet = MapUtils.getApply(data, "conditionSet", Map.class, ConditionSet::new);
    }

    @Override
    public FunctionResult evaluate(BaseElement element, Map<String, Object> customerInput, String id, ScriptEngine scriptEngine) {
        return new FunctionResult(getConditionSet().evaluate(customerInput));
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }
}
