package de.aivot.GoverBackend.models.functions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.utils.MapUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FunctionNoCode extends Function {
    private ConditionSet conditionSet;

    public FunctionNoCode(Map<String, Object> data) {
        super(data);
        conditionSet = MapUtils.getApply(data, "conditionSet", Map.class, ConditionSet::new);
    }

    @Override
    public FunctionResult evaluate(String idPrefix, BaseElement element, BaseElementDerivationContext context) {
        if (conditionSet == null) {
            return new FunctionResult(null, true);
        }
        return new FunctionResult(conditionSet.evaluate(idPrefix, context), true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FunctionNoCode that = (FunctionNoCode) o;
        return Objects.equals(conditionSet, that.conditionSet) && super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(conditionSet);
        result = 31 * result + super.hashCode();
        return result;
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return conditionSet == null || (conditionSet.getConditions().isEmpty() && conditionSet.getConditionsSets().isEmpty());
    }

    @NotNull
    @Override
    public Set<String> getReferencedIds() {
        if (conditionSet == null) {
            return new HashSet<>();
        }

        return conditionSet.getReferencedIds();
    }
}
