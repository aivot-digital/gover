package de.aivot.GoverBackend.models.functions;

import java.util.Map;

public abstract class Function {
    private String requirements;

    protected Function(Map<String, Object> data) {
        requirements = (String) data.get("requirements");
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
}
