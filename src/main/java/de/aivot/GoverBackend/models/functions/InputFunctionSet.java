package de.aivot.GoverBackend.models.functions;

import com.sun.istack.Nullable;

import java.util.Map;

public class InputFunctionSet extends FunctionSet {
    private Function isValid;
    private Function isDisabled;
    private Function isRequired;
    private FunctionCode computeValue;

    public InputFunctionSet(Map<String, Object> data) {
        super(data);

        if (data != null) {
            if (data.containsKey("isValid")) {
                Map<String, Object> func = (Map<String, Object>) data.get("isValid");
                if (func.containsKey("isValid")) {
                    isValid = new FunctionCode(func);
                } else {
                    isValid = new FunctionNoCode(func);
                }
            }

            if (data.containsKey("isDisabled")) {
                Map<String, Object> func = (Map<String, Object>) data.get("isDisabled");
                if (func.containsKey("isDisabled")) {
                    isDisabled = new FunctionCode(func);
                } else {
                    isDisabled = new FunctionNoCode(func);
                }
            }

            if (data.containsKey("isRequired")) {
                Map<String, Object> func = (Map<String, Object>) data.get("isRequired");
                if (func.containsKey("isRequired")) {
                    isDisabled = new FunctionCode(func);
                } else {
                    isDisabled = new FunctionNoCode(func);
                }
            }

            computeValue = data.containsKey("computeValue") ? new FunctionCode((Map<String, Object>) data.get("computeValue")) : null;
        }
    }

    public Function getIsValid() {
        return isValid;
    }

    public void setIsValid(Function isValid) {
        this.isValid = isValid;
    }

    public Function getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Function isDisabled) {
        this.isDisabled = isDisabled;
    }

    public Function getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Function isRequired) {
        this.isRequired = isRequired;
    }

    public FunctionCode getComputeValue() {
        return computeValue;
    }

    public void setComputeValue(FunctionCode computeValue) {
        this.computeValue = computeValue;
    }
}
