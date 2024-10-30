package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Map;
import java.util.Objects;

public class FunctionNoCode extends Function {
    private ConditionSet conditionSet;

    public FunctionNoCode(Map<String, Object> data) {
        super(data);
        conditionSet = MapUtils.getApply(data, "conditionSet", Map.class, ConditionSet::new);
    }

    @Override
    public FunctionResult evaluate(String idPrefix, RootElement root, BaseElement element, Map<String, Object> customerInput, ScriptEngine scriptEngine) {
        if (conditionSet == null) {
            return new FunctionResult(null, true);
        }
        return new FunctionResult(conditionSet.evaluate(idPrefix, root, customerInput, scriptEngine), true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FunctionNoCode that = (FunctionNoCode) o;

        return Objects.equals(conditionSet, that.conditionSet);
    }

    @Override
    public int hashCode() {
        return conditionSet != null ? conditionSet.hashCode() : 0;
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }
}
