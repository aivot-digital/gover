package de.aivot.GoverBackend.models.functions;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Map;

public abstract class Function {
    private String requirements;

    protected Function(Map<String, Object> data) {
        requirements = MapUtils.getString(data, "requirements");
    }

    public abstract FunctionResult evaluate(String idPrefix, RootElement root, BaseElement element, Map<String, Object> customerInput, ScriptEngine scriptEngine);

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
}
