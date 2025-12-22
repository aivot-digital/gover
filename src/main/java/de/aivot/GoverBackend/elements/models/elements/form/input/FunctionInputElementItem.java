package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.models.functions.conditions.ConditionSet;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class FunctionInputElementItem implements Serializable {
    @Nullable
    private FunctionInputElementItemType type;

    @Nullable
    private JavascriptCode code;

    @Nullable
    private ConditionSet conditionSet;

    @Nullable
    private NoCodeOperand noCode;

    // region constructors

    // Empty constructor for serialization
    public FunctionInputElementItem() {
    }

    // Full constructor
    public FunctionInputElementItem(@Nullable FunctionInputElementItemType type,
                                    @Nullable JavascriptCode code,
                                    @Nullable ConditionSet conditionSet,
                                    @Nullable NoCodeOperand noCode) {
        this.type = type;
        this.code = code;
        this.conditionSet = conditionSet;
        this.noCode = noCode;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FunctionInputElementItem that = (FunctionInputElementItem) o;
        return type == that.type && Objects.equals(code, that.code) && Objects.equals(conditionSet, that.conditionSet) && Objects.equals(noCode, that.noCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, code, conditionSet, noCode);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public FunctionInputElementItemType getType() {
        return type;
    }

    public FunctionInputElementItem setType(@Nullable FunctionInputElementItemType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public JavascriptCode getCode() {
        return code;
    }

    public FunctionInputElementItem setCode(@Nullable JavascriptCode code) {
        this.code = code;
        return this;
    }

    @Nullable
    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public FunctionInputElementItem setConditionSet(@Nullable ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
        return this;
    }

    @Nullable
    public NoCodeOperand getNoCode() {
        return noCode;
    }

    public FunctionInputElementItem setNoCode(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
        return this;
    }

    // endregion

    public enum FunctionInputElementItemType {
        NoCode,
        ConditionSet,
        Javascript,
    }
}
