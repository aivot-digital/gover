package de.aivot.GoverBackend.models.functions.conditions;

import java.util.Map;

public class ConditionOperandValue extends ConditionOperand {
    private String value;

    public ConditionOperandValue(Map<String, Object> data) {
        value = (String) data.get("value");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
