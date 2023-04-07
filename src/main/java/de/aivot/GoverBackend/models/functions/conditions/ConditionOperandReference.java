package de.aivot.GoverBackend.models.functions.conditions;

import java.util.Map;

public class ConditionOperandReference extends ConditionOperand {
    private String id;

    public ConditionOperandReference(Map<String, Object> data) {
        id = (String) data.get("id");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
