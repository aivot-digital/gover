package de.aivot.GoverBackend.models.functions;

import com.sun.istack.Nullable;

import java.util.Map;

public class FunctionCode extends Function {
    private Map<String, String> functions;
    private String mainFunction;

    public FunctionCode(Map<String, Object> data) {
        super(data);
        functions = (Map<String, String>) data.get("functions");
        mainFunction = (String) data.get("mainFunction");
    }

    @Nullable
    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }

    @Nullable
    public String getMainFunction() {
        return mainFunction;
    }

    public void setMainFunction(String mainFunction) {
        this.mainFunction = mainFunction;
    }
}
