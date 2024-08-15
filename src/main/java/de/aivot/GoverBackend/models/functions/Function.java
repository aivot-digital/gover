package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Map;
import java.util.Objects;

public abstract class Function {
    private String requirements;

    protected Function(Map<String, Object> data) {
        requirements = MapUtils.getString(data, "requirements");
    }

    protected Function() {
    }

    public abstract FunctionResult evaluate(String idPrefix, RootElement root, BaseElement element, Map<String, Object> customerInput, ScriptEngine scriptEngine);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Function function = (Function) o;

        return Objects.equals(requirements, function.requirements);
    }

    @Override
    public int hashCode() {
        return requirements != null ? requirements.hashCode() : 0;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
}
