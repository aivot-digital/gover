package de.aivot.GoverBackend.models.functions;

import com.sun.istack.Nullable;

import java.util.Map;

public class FunctionSet {
    private Function isVisible;
    private FunctionCode patchElement;

    public FunctionSet(Map<String, Object> data) {
        if (data != null) {
            if (data.containsKey("isVisible")) {
                Map<String, Object> func = (Map<String, Object>) data.get("isVisible");
                if (func.containsKey("functions")) {
                    isVisible = new FunctionCode(func);
                } else {
                    isVisible = new FunctionNoCode(func);
                }
            }

            patchElement = data.containsKey("patchElement") ? new FunctionCode((Map<String, Object>) data.get("patchElement")) : null;
        }
    }

    @Nullable
    public Function getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Function isVisible) {
        this.isVisible = isVisible;
    }

    @Nullable
    public FunctionCode getPatchElement() {
        return patchElement;
    }

    public void setPatchElement(FunctionCode patchElement) {
        this.patchElement = patchElement;
    }
}
